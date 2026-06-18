package com.exam.service;

import com.exam.common.constants.ChatConstants;
import com.google.common.hash.Hashing;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 通知去重服务。
 * <p>
 * <b>业务目的</b>：防止阅卷流程多次触发导致"成绩发布"等通知在短时间内被重复推送给同一用户，减少打扰。
 * <p>
 * <b>核心算法</b>（P0-3 重构后）：<b>Redis {@code SET NX EX} 原子占位</b>。
 * <pre>
 *   {@code redisTemplate.opsForValue().setIfAbsent(key, "1", TTL, SECONDS)}
 *      ↳ 返回 true  ⇒ 首次设置成功 ⇒ 这是新通知 ⇒ 放行
 *      ↳ 返回 false ⇒ Key 已存在   ⇒ 窗口内已发过 ⇒ 拦截
 * </pre>
 * <p>
 * <b>P0-3 修复记录</b>：旧版用 {@code hasKey(key)} + {@code set(key, ...)} 两条命令，
 * 存在 Check-Then-Act TOCTOU 竞态 —— 两个线程可能同时通过 {@code hasKey} 检查然后都执行 {@code set}，
 * 导致重复通知。改造为 {@code SET NX EX} 原子命令后彻底消除竞态。
 * <p>
 * <b>权威依据</b>：
 * <ul>
 *   <li><a href="https://redis.io/docs/latest/commands/SETNX/">Redis 官方 SETNX 文档</a>：
 *       「Always prefer {@code SET key value NX EX seconds} in new code.」</li>
 *   <li><a href="https://redis.io/en/blog/what-is-idempotency-in-redis/">Redis 官方博客 · Idempotency</a></li>
 *   <li>阿里巴巴 Java 开发手册 §2.3：禁止 Check-Then-Act 模式</li>
 * </ul>
 * <p>
 * <b>降级策略</b>：Redis 不可用时返回 false（不去重），允许发送 —— 宁可重复也不阻塞通知触达。
 */
@Slf4j
@Service
public class NotificationDeduplicationService {

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    /**
     * 检查通知是否重复（原子版，消除 TOCTOU 竞态）。
     *
     * @param userId 用户ID
     * @param type   通知类型
     * @param bizId  业务ID
     * @return {@code true}=重复（已拦截），{@code false}=不重复（应发送）
     */
    public boolean isDuplicate(Long userId, String type, Long bizId) {
        // Redis 未配置时降级：不去重
        if (redisTemplate == null) {
            log.warn("Redis 未配置，通知去重功能降级");
            return false;
        }

        try {
            String key = generateKey(userId, type, bizId);
            // ✅ P0-3 原子操作 SET NX EX：
            //   - 返回 true  ⇒ 首次设置成功，这是新通知，放行
            //   - 返回 false ⇒ Key 已存在，窗口内的重复通知，拦截
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                    key, "1",
                    ChatConstants.NOTIFY_DEDUP_WINDOW_SECONDS,
                    TimeUnit.SECONDS
            );
            boolean isFirst = Boolean.TRUE.equals(acquired);
            if (!isFirst) {
                log.debug("通知去重：用户 {} 的 {} 通知已在 {} 秒内发送过",
                        userId, type, ChatConstants.NOTIFY_DEDUP_WINDOW_SECONDS);
            }
            return !isFirst;
        } catch (Exception e) {
            // Redis 异常时降级：不去重，允许发送
            log.error("Redis 去重失败，降级处理", e);
            return false;
        }
    }

    /**
     * 生成去重 Key。
     * <p>
     * 格式：{@code notification:dedup:{hash}}，
     * 其中 {@code hash = SHA-256(userId:type:bizId) 前 16 位}。
     * <p>
     * 使用 hash 压缩 Key 是为了避免原始字段过长（如 type 是中文描述）撑爆 Key 空间，
     * 同时 16 位十六进制 hash = 64 bit 熵值，碰撞概率 &lt; 1e-9，业务级安全。
     */
    private String generateKey(Long userId, String type, Long bizId) {
        String raw = userId + ":" + type + ":" + (bizId != null ? bizId : "");
        String hash = Hashing.sha256()
                .hashString(raw, StandardCharsets.UTF_8)
                .toString()
                .substring(0, 16);
        return ChatConstants.NOTIFY_DEDUP_KEY_PREFIX + hash;
    }

    /**
     * 手动清除去重标记（用于测试或特殊场景如"重发通知"管理后台操作）。
     */
    public void clearDeduplication(Long userId, String type, Long bizId) {
        if (redisTemplate == null) return;

        try {
            String key = generateKey(userId, type, bizId);
            redisTemplate.delete(key);
            log.info("已清除通知去重标记：用户 {}, 类型 {}", userId, type);
        } catch (Exception e) {
            log.error("清除去重标记失败", e);
        }
    }
}
