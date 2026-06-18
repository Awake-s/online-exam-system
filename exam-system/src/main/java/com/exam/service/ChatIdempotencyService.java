package com.exam.service;

import com.exam.common.constants.ChatConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 聊天消息幂等键服务
 * <p>
 * 目的：客户端网络抖动重试时，同一 clientMsgId 只持久化一次，
 * 彻底根治"发消息没反应 → 用户重复点击 → DB 重复入库"问题。
 * <p>
 * 核心算法（L3-M1-1 修复后）：<b>Redis SETNX 原子占位</b>
 * <pre>
 *   1. tryAcquireSlot      : SETNX 占位 "PENDING"，抢到才继续业务
 *   2. 插入 DB 成功         : markProcessed 覆盖为真实 messageId
 *   3. 插入 DB 失败         : releaseSlot 清除占位，允许立即重试
 *   4. 没抢到              : getExistingMessageId 取已处理结果（若仍 PENDING 则返回 null）
 * </pre>
 * <p>
 * Redis 数据结构：
 * <pre>
 *   key:   chat:idem:{userId}:{clientMsgId}
 *   value: "PENDING" | {messageId}
 *   TTL:   30 秒
 * </pre>
 * <p>
 * 降级策略：Redis 不可用时自动降级为"不去重"，保证核心功能可用。
 */
@Slf4j
@Service
public class ChatIdempotencyService {

    /** 占位值：抢到 slot 后尚未产生真实 messageId 时的中间态 */
    private static final String PENDING_VALUE = "PENDING";

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    /**
     * L3-M1-1：原子占位，消除 check-then-act 并发竞态。
     * <p>
     * 使用 Redis {@code SET NX EX} 命令：
     * <ul>
     *   <li>返回 {@code true}  → 当前请求抢到 slot，必须执行业务并在完成后调 {@link #markProcessed} 或失败时调 {@link #releaseSlot}</li>
     *   <li>返回 {@code false} → 已有并发请求抢占，调用方应调 {@link #getExistingMessageId} 取结果</li>
     * </ul>
     * <p>
     * 无 clientMsgId / Redis 不可用 / 异常 → 统一降级返回 {@code true}（走正常插库路径，牺牲幂等保可用）。
     *
     * @return 是否抢到 slot
     */
    public boolean tryAcquireSlot(Long userId, String clientMsgId) {
        if (!isValidInput(userId, clientMsgId)) return true;  // 无幂等键：直接放行
        if (redisTemplate == null) return true;               // Redis 未配置：降级放行
        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                    buildKey(userId, clientMsgId),
                    PENDING_VALUE,
                    ChatConstants.IDEMPOTENCY_WINDOW_SECONDS,
                    TimeUnit.SECONDS
            );
            return Boolean.TRUE.equals(acquired);
        } catch (Exception e) {
            log.warn("幂等占位失败，降级放行: {}", e.getMessage());
            return true;  // Redis 故障：降级放行（宁可重复也不阻塞）
        }
    }

    /**
     * L3-M1-1：业务失败时释放占位，允许用户立即重试（否则会被卡 30 秒）。
     * <p>
     * 仅在抢到 slot 后的业务异常路径调用；正常成功路径用 {@link #markProcessed} 覆盖为真实 id。
     */
    public void releaseSlot(Long userId, String clientMsgId) {
        if (!isValidInput(userId, clientMsgId)) return;
        if (redisTemplate == null) return;
        try {
            redisTemplate.delete(buildKey(userId, clientMsgId));
        } catch (Exception e) {
            log.warn("幂等占位释放失败（TTL 兜底 30s 自动过期）: {}", e.getMessage());
        }
    }

    /**
     * 查询已处理的 messageId。
     * <p>
     * 返回语义：
     * <ul>
     *   <li>{@code null}      → 从未处理 / PENDING 中 / Redis 故障</li>
     *   <li>正整数             → 之前处理完成的 messageId</li>
     * </ul>
     * PENDING 状态被当作 null 返回（调用方不该把 PENDING 当作"已完成"）。
     */
    public Long getExistingMessageId(Long userId, String clientMsgId) {
        if (!isValidInput(userId, clientMsgId)) return null;
        if (redisTemplate == null) return null;
        try {
            String value = redisTemplate.opsForValue().get(buildKey(userId, clientMsgId));
            if (value == null || PENDING_VALUE.equals(value)) return null;  // PENDING 视为未完成
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.warn("幂等键 value 非法（非数字）: userId={}, clientMsgId={}", userId, clientMsgId);
            return null;
        } catch (Exception e) {
            log.warn("幂等键查询失败，降级放行: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 记录 clientMsgId → messageId 的最终映射，覆盖 PENDING 占位，TTL 刷新为 30 秒。
     */
    public void markProcessed(Long userId, String clientMsgId, Long messageId) {
        if (!isValidInput(userId, clientMsgId) || messageId == null) return;
        if (redisTemplate == null) return;
        try {
            redisTemplate.opsForValue().set(
                    buildKey(userId, clientMsgId),
                    String.valueOf(messageId),
                    ChatConstants.IDEMPOTENCY_WINDOW_SECONDS,
                    TimeUnit.SECONDS
            );
        } catch (Exception e) {
            log.warn("幂等键写入失败，不影响业务: {}", e.getMessage());
        }
    }

    /**
     * 校验输入合法性：防止攻击者用超长 clientMsgId 撑爆 Redis key 空间
     */
    private boolean isValidInput(Long userId, String clientMsgId) {
        return userId != null
                && clientMsgId != null
                && !clientMsgId.isEmpty()
                && clientMsgId.length() <= ChatConstants.CLIENT_MSG_ID_MAX_LENGTH;
    }

    private String buildKey(Long userId, String clientMsgId) {
        return ChatConstants.IDEMPOTENCY_KEY_PREFIX + userId + ":" + clientMsgId;
    }
}
