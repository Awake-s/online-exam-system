package com.exam.security;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.exam.common.constants.ChatConstants;

/**
 * Token 黑名单服务：基于 Redis 存储，避免服务重启后已登出的 token 被复用。
 * 为保证 Redis 故障时登出功能仍可工作，保留内存实现作为降级兜底。
 */
@Component
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);
    /**
     * Key 前缀集中管理 —— 引用 {@link ChatConstants#AUTH_BLACKLIST_KEY_PREFIX}，
     * 避免在业务类散落硬编码字符串（阿里开发规约 §6.1）。
     */
    private static final String REDIS_KEY_PREFIX = ChatConstants.AUTH_BLACKLIST_KEY_PREFIX;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    // 内存降级存储（Redis 不可用时启用）
    private final ConcurrentMap<String, Long> fallbackBlacklist = new ConcurrentHashMap<>();

    public void add(String token, long expirationTimeMs) {
        if (token == null || token.isEmpty()) return;
        long ttlMillis = expirationTimeMs - System.currentTimeMillis();
        if (ttlMillis <= 0) return;

        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(REDIS_KEY_PREFIX + token, "1", ttlMillis, TimeUnit.MILLISECONDS);
                return;
            } catch (Exception e) {
                log.warn("Redis 写入黑名单失败，降级至内存: {}", e.getMessage());
            }
        }
        fallbackBlacklist.put(token, expirationTimeMs);
    }

    public boolean isBlacklisted(String token) {
        if (token == null || token.isEmpty()) return false;

        if (redisTemplate != null) {
            try {
                Boolean exists = redisTemplate.hasKey(REDIS_KEY_PREFIX + token);
                if (Boolean.TRUE.equals(exists)) return true;
            } catch (Exception e) {
                log.warn("Redis 查询黑名单失败，降级至内存: {}", e.getMessage());
            }
        }

        Long expiry = fallbackBlacklist.get(token);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            fallbackBlacklist.remove(token);
            return false;
        }
        return true;
    }

    /**
     * 定时清理内存降级存储中已过期的条目。
     * Redis 条目通过 TTL 自动过期，无需清理。
     */
    @Scheduled(fixedRate = 3600000)
    public void cleanup() {
        long now = System.currentTimeMillis();
        fallbackBlacklist.entrySet().removeIf(e -> now > e.getValue());
    }
}
