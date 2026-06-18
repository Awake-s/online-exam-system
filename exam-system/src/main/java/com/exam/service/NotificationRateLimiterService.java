package com.exam.service;

import com.google.common.util.concurrent.RateLimiter;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通知限流服务（D3 分布式版）。
 * <p>
 * <b>策略</b>：每个用户每分钟最多接收 10 条通知。
 * <p>
 * <b>算法</b>：Redis + Lua <i>固定窗口计数器</i>（{@code INCR + EXPIRE}），
 * 原子执行，支持集群部署下的准确限流。
 * <pre>
 *   Redis Key: notification:ratelimit:{userId}
 *   Value:     当前窗口内已发送条数
 *   TTL:       60 秒（窗口大小）
 * </pre>
 * <p>
 * <b>P0-3 对齐</b>：与 {@link NotificationDeduplicationService} 共用 Redis，
 * 既保证分布式去重又保证分布式限流，一致性达生产级。
 * <p>
 * <b>降级策略（多层防御）</b>：
 * <ol>
 *   <li>首选 Redis 分布式限流（集群安全）</li>
 *   <li>Redis 未配置 / 异常 → 自动退化到本地 Guava RateLimiter（单机）</li>
 *   <li>降级时日志告警，保持业务不中断</li>
 * </ol>
 * <p>
 * <b>权威依据</b>：
 * <ul>
 *   <li><a href="https://redis.io/docs/latest/develop/use/patterns/distributed-locks/">Redis 官方 · Patterns for Rate Limiting</a></li>
 *   <li>Cloudflare / Stripe 的工程博客多次推荐 INCR+EXPIRE 固定窗口模式</li>
 * </ul>
 */
@Slf4j
@Service
public class NotificationRateLimiterService {

    /** 每分钟允许的通知条数上限 */
    private static final long PERMITS_PER_MINUTE = 10L;

    /** 限流窗口大小（秒） */
    private static final long WINDOW_SECONDS = 60L;

    /** Redis Key 前缀 */
    private static final String REDIS_KEY_PREFIX = "notification:ratelimit:";

    /**
     * Lua 脚本：原子限流判断
     * <pre>
     * 入参：
     *   KEYS[1]：限流 Key
     *   ARGV[1]：容量上限（每窗口允许的最大请求数）
     *   ARGV[2]：窗口 TTL（秒）
     * 出参：
     *   1 = 放行，0 = 拒绝
     * </pre>
     */
    private static final String RATE_LIMIT_LUA =
            "local current = redis.call('GET', KEYS[1]) " +
            "if current == false then " +
            "  redis.call('SET', KEYS[1], 1, 'EX', ARGV[2]) " +
            "  return 1 " +
            "end " +
            "if tonumber(current) < tonumber(ARGV[1]) then " +
            "  redis.call('INCR', KEYS[1]) " +
            "  return 1 " +
            "else " +
            "  return 0 " +
            "end";

    private DefaultRedisScript<Long> rateLimitScript;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    /** 本地降级限流器（当 Redis 不可用时使用，单机兜底） */
    private final ConcurrentHashMap<Long, RateLimiter> fallbackLimiters = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        rateLimitScript = new DefaultRedisScript<>();
        rateLimitScript.setScriptText(RATE_LIMIT_LUA);
        rateLimitScript.setResultType(Long.class);
    }

    /**
     * 检查用户是否超过限流（Redis 优先，失败降级到本地）。
     * @param userId 用户ID
     * @return true=允许发送，false=超过限流
     */
    public boolean tryAcquire(Long userId) {
        // 1. 首选 Redis 分布式限流
        if (redisTemplate != null) {
            try {
                String key = REDIS_KEY_PREFIX + userId;
                Long result = redisTemplate.execute(
                        rateLimitScript,
                        Collections.singletonList(key),
                        String.valueOf(PERMITS_PER_MINUTE),
                        String.valueOf(WINDOW_SECONDS)
                );
                boolean allowed = result != null && result == 1L;
                if (!allowed) {
                    log.warn("⚠️ [Redis] 用户 {} 通知发送超过限流（每分钟 {} 条）", userId, PERMITS_PER_MINUTE);
                }
                return allowed;
            } catch (Exception e) {
                // Redis 异常时降级到本地限流，保业务不中断
                log.error("Redis 限流失败，降级到本地限流。用户 {}", userId, e);
            }
        }

        // 2. 降级：本地 Guava RateLimiter（单机级别）
        RateLimiter limiter = fallbackLimiters.computeIfAbsent(
                userId,
                k -> RateLimiter.create((double) PERMITS_PER_MINUTE / WINDOW_SECONDS)
        );
        boolean allowed = limiter.tryAcquire();
        if (!allowed) {
            log.warn("⚠️ [本地降级] 用户 {} 通知发送超过限流（每分钟 {} 条）", userId, PERMITS_PER_MINUTE);
        }
        return allowed;
    }

    /**
     * 重置用户限流器（用于测试或特殊场景如管理员重发）。
     */
    public void resetLimiter(Long userId) {
        // 清 Redis
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(REDIS_KEY_PREFIX + userId);
            } catch (Exception e) {
                log.error("重置 Redis 限流器失败，用户 {}", userId, e);
            }
        }
        // 清本地
        fallbackLimiters.remove(userId);
        log.info("已重置用户 {} 的限流器", userId);
    }

    /**
     * 清理所有本地降级限流器（定期清理，防止内存泄漏）。
     * <p>Redis 侧 TTL 自动过期，无需手动清理。
     */
    public void clearAllLimiters() {
        int size = fallbackLimiters.size();
        fallbackLimiters.clear();
        if (size > 0) {
            log.info("已清理 {} 个本地降级限流器", size);
        }
    }

    /**
     * 获取本地降级限流器数量（监控用，Redis 侧的由 Redis 自己管理）
     */
    public int getLimiterCount() {
        return fallbackLimiters.size();
    }
}
