package com.exam.service;

import com.exam.common.constants.ChatConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * L3-M0-4：撤回消息"重新编辑"草稿缓存服务。
 * <p>
 * <b>业务场景（对齐微信/QQ 2 分钟内"重新编辑"）</b>：
 * 用户发送消息后立即反悔撤回，撤回成功后占位符旁出现"重新编辑"按钮，点击将原文本
 * 回填至输入框以便用户修正。此功能对"手抖误发"和"发错人"是关键体验加分项。
 * <p>
 * <b>存储策略</b>：Redis 主存 + 进程内存降级（与 {@link TokenBlacklistService} 同模式）。
 * <ul>
 *   <li>Redis 可用 → 写入 Redis {@code chat:recall:draft:{messageId}}，TTL 2 分钟</li>
 *   <li>Redis 不可用 → 写入本地 ConcurrentHashMap，单节点 fallback 不丢 UX</li>
 * </ul>
 * <p>
 * <b>安全设计</b>：
 * <ul>
 *   <li>只保留消息原文，不保留发送者/接收者信息 —— 调用方负责鉴权</li>
 *   <li>TTL 与 MESSAGE_RECALL_WINDOW_SECONDS 同值（2min），过期后原文彻底不可恢复</li>
 *   <li>内存降级时带过期时间戳 + 惰性清理，防止内存泄漏</li>
 *   <li>value 只存 content，不存 messageType（非文字消息调用方层面就不保存）</li>
 * </ul>
 */
@Slf4j
@Service
public class ChatRecallDraftService {

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    /** 内存降级存储（Redis 不可用时启用），value = 原文 + 过期时间戳 */
    private final Map<Long, FallbackEntry> fallback = new ConcurrentHashMap<>();

    /**
     * 保存撤回草稿。
     * <p>
     * 调用时机：{@code ChatServiceImpl.recallMessage} 在 softDeleteMessage 之前调用
     * （因为 softDelete 会使 content 物理保留但 deleted_at 非空，我们在逻辑上已视为"撤回"，
     * 此时原文进入 2 分钟草稿缓存，供发送者"重新编辑"用）。
     *
     * @param messageId 消息 ID
     * @param content   消息原文（非 null 非空）
     */
    public void saveDraft(Long messageId, String content) {
        if (messageId == null || content == null || content.isEmpty()) return;
        long ttl = ChatConstants.RECALL_DRAFT_TTL_SECONDS;
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(buildKey(messageId), content, ttl, TimeUnit.SECONDS);
                return;
            } catch (Exception e) {
                log.warn("Redis 写入撤回草稿失败，降级至内存: messageId={}, err={}", messageId, e.getMessage());
            }
        }
        // 降级：写入本地内存，记录绝对过期时间
        fallback.put(messageId, new FallbackEntry(content, Instant.now().getEpochSecond() + ttl));
        cleanupExpired();
    }

    /**
     * 读取撤回草稿。
     * <p>
     * 返回 {@code null} 的情况：
     * <ul>
     *   <li>从未保存过（非文字消息 / 超时后才撤回）</li>
     *   <li>已过期（TTL 耗尽）</li>
     *   <li>Redis + 内存均无</li>
     * </ul>
     *
     * @return 撤回前的原文，或 null 表示草稿不存在/已过期
     */
    public String getDraft(Long messageId) {
        if (messageId == null) return null;
        // 优先从 Redis 读
        if (redisTemplate != null) {
            try {
                String value = redisTemplate.opsForValue().get(buildKey(messageId));
                if (value != null) return value;
            } catch (Exception e) {
                log.warn("Redis 读取撤回草稿失败，尝试内存: messageId={}, err={}", messageId, e.getMessage());
            }
        }
        // 内存兜底
        FallbackEntry entry = fallback.get(messageId);
        if (entry == null) return null;
        if (entry.expireAt < Instant.now().getEpochSecond()) {
            fallback.remove(messageId);  // 惰性清理
            return null;
        }
        return entry.content;
    }

    /**
     * 主动删除草稿（草稿回填成功后，防止用户反复使用同一 messageId 多次回填）。
     */
    public void deleteDraft(Long messageId) {
        if (messageId == null) return;
        if (redisTemplate != null) {
            try { redisTemplate.delete(buildKey(messageId)); } catch (Exception ignored) { /* 容忍 */ }
        }
        fallback.remove(messageId);
    }

    private String buildKey(Long messageId) {
        return ChatConstants.RECALL_DRAFT_KEY_PREFIX + messageId;
    }

    /**
     * 惰性清理过期的内存条目，防止长期累积。
     * <p>
     * 每次 saveDraft 时顺带扫一次；正常使用场景下 fallback Map 大小极小（<100）。
     */
    private void cleanupExpired() {
        long now = Instant.now().getEpochSecond();
        fallback.entrySet().removeIf(e -> e.getValue().expireAt < now);
    }

    /** 内存降级条目：原文 + 绝对过期时间（秒） */
    private static final class FallbackEntry {
        final String content;
        final long expireAt;
        FallbackEntry(String content, long expireAt) {
            this.content = content;
            this.expireAt = expireAt;
        }
    }
}
