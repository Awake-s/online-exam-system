package com.exam.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 登录失败次数限制器（防暴力破解）
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MS = 15 * 60 * 1000;

    private final ConcurrentMap<String, long[]> attempts = new ConcurrentHashMap<>();

    /**
     * 检查账号是否被锁定
     */
    public boolean isLocked(String username) {
        long[] info = attempts.get(username);
        if (info == null) return false;
        if (info[0] >= MAX_ATTEMPTS && System.currentTimeMillis() < info[1]) {
            return true;
        }
        if (info[0] >= MAX_ATTEMPTS && System.currentTimeMillis() >= info[1]) {
            attempts.remove(username);
            return false;
        }
        return false;
    }

    /**
     * 获取剩余锁定分钟数
     */
    public long getRemainingLockMinutes(String username) {
        long[] info = attempts.get(username);
        if (info == null) return 0;
        long remaining = info[1] - System.currentTimeMillis();
        return remaining > 0 ? (remaining / 60000) + 1 : 0;
    }

    public void recordFailure(String username) {
        long[] info = attempts.computeIfAbsent(username, k -> new long[]{0, 0});
        info[0]++;
        if (info[0] >= MAX_ATTEMPTS) {
            info[1] = System.currentTimeMillis() + LOCK_DURATION_MS;
        }
    }

    public void recordSuccess(String username) {
        attempts.remove(username);
    }

    @Scheduled(fixedRate = 3600000)
    public void cleanup() {
        long now = System.currentTimeMillis();
        attempts.entrySet().removeIf(e -> e.getValue()[0] >= MAX_ATTEMPTS && now >= e.getValue()[1]);
    }
}
