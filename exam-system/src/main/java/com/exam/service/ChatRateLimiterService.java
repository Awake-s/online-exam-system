package com.exam.service;

import com.exam.common.constants.ChatConstants;
import com.exam.common.constants.RoleConstants;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天消息速率限制（按角色分级）
 * <p>
 * 策略：
 * <ul>
 *   <li>管理员：{@link ChatConstants#RATE_LIMIT_ADMIN_PER_SECOND} 条/秒（批量通知）</li>
 *   <li>教师：  {@link ChatConstants#RATE_LIMIT_TEACHER_PER_SECOND} 条/秒（答疑）</li>
 *   <li>学生：  {@link ChatConstants#RATE_LIMIT_STUDENT_PER_SECOND} 条/秒（防刷屏）</li>
 * </ul>
 * <p>
 * 架构与 {@link NotificationRateLimiterService} 保持一致，便于统一维护。
 * Guava SmoothBursty 默认允许攒满 1 秒的令牌作为突发容量。
 */
@Slf4j
@Service
public class ChatRateLimiterService {

    /** 每用户的 RateLimiter 缓存（userId → RateLimiter） */
    private final ConcurrentHashMap<Long, RateLimiter> userLimiters = new ConcurrentHashMap<>();

    /**
     * 尝试获取发送许可（非阻塞）。
     * <p>
     * L3-M2-2：角色变更感知 —— 每次调用都检查缓存的 Limiter 配额是否匹配当前 role，
     * 不匹配则重建。这保证用户被升降级后（如学生→管理员）配额立即生效。
     *
     * @param userId 用户 ID
     * @param roleId 用户角色 ID（决定配额），null 时使用默认配额
     * @return true=允许发送，false=超过限流
     */
    public boolean tryAcquire(Long userId, Long roleId) {
        if (userId == null) return false;
        double expectedRate = resolveRateForRole(roleId);
        RateLimiter limiter = userLimiters.compute(userId, (k, existing) -> {
            // L3-M2-2：若 Limiter 不存在，或配额与当前角色不匹配，重建。
            // 用 0.001 容差比较 double，规避 Guava RateLimiter 内部精度问题。
            if (existing == null || Math.abs(existing.getRate() - expectedRate) > 0.001) {
                return RateLimiter.create(expectedRate);
            }
            return existing;
        });
        boolean allowed = limiter.tryAcquire();
        if (!allowed) {
            // L3-M2-1：日志加上 retry_after 字段便于运维监控 / 未来扩展 HTTP 429 + Retry-After 头
            log.warn("⚠️ 用户 {} (角色 {}) 发送消息超过限流 ({} 条/秒), 建议 retry_after={}s",
                    userId, roleId, limiter.getRate(),
                    ChatConstants.RATE_LIMIT_RETRY_AFTER_SECONDS);
        }
        return allowed;
    }

    /**
     * 按角色解析对应的每秒配额
     */
    private double resolveRateForRole(Long roleId) {
        if (RoleConstants.ADMIN_ROLE_ID.equals(roleId)) {
            return ChatConstants.RATE_LIMIT_ADMIN_PER_SECOND;
        } else if (RoleConstants.TEACHER_ROLE_ID.equals(roleId)) {
            return ChatConstants.RATE_LIMIT_TEACHER_PER_SECOND;
        } else if (RoleConstants.STUDENT_ROLE_ID.equals(roleId)) {
            return ChatConstants.RATE_LIMIT_STUDENT_PER_SECOND;
        }
        return ChatConstants.RATE_LIMIT_DEFAULT_PER_SECOND;
    }

    /**
     * 清理所有限流器缓存（定期任务调用，防内存泄漏）
     */
    public void clearAllLimiters() {
        int size = userLimiters.size();
        userLimiters.clear();
        log.info("已清理 {} 个聊天限流器", size);
    }

    /**
     * 获取当前限流器数量（监控用）
     */
    public int getLimiterCount() {
        return userLimiters.size();
    }
}
