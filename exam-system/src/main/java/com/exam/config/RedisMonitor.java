package com.exam.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * Redis 运行时监控器 —— 定时采样关键健康指标。
 * <p>
 * <b>业务目的</b>：Redis 最危险的故障不是"宕机"（业务已有降级），而是"慢而不死"
 * —— 响应从 0.1ms 劣化到 200ms，业务未触发超时但用户体验已崩。本监控器按分钟级
 * 持续采样命中率、内存、连接数，为生产故障排查提供数据基础。
 * <p>
 * <b>采样指标</b>：
 * <ol>
 *   <li>{@code keyspace_hits / keyspace_misses} → 缓存命中率（低于 80% 告警）</li>
 *   <li>{@code used_memory} → 内存占用（超过 400MB 告警，为 512MB maxmemory 留出缓冲）</li>
 *   <li>{@code connected_clients} → 当前连接数</li>
 *   <li>{@code dbSize} → 当前 db 的 Key 总数</li>
 * </ol>
 * <p>
 * <b>兼容性保护</b>：
 * <ul>
 *   <li>Redis 未配置 / 连接失败 → 静默跳过（仅 DEBUG 日志）</li>
 *   <li>采样结果解析失败（Redis 3.0.504 与 Spring Data Redis 4.x 间极少数 INFO 字段名差异）
 *       → 用 {@link #parseLongSafe} 兜底返回 0，不抛异常</li>
 *   <li>RedisConnection 关闭异常 → finally 吞掉，不传播</li>
 * </ul>
 * <p>
 * <b>调度来源</b>：依赖 {@code @EnableScheduling}（已在
 * {@link com.exam.ExamApplication} 启用）。
 * <p>
 * <b>权威参考</b>：
 * <ul>
 *   <li><a href="https://redis.io/docs/latest/commands/info/">Redis 官方 INFO 命令文档</a></li>
 *   <li><a href="https://developer.aliyun.com/article/698980">阿里云 · Redis 三大经典问题（穿透/雪崩/击穿）与监控</a></li>
 * </ul>
 *
 * @author Cascade
 * @since Redis 进阶优化指南 · 第六级
 */
@Slf4j
@Component
public class RedisMonitor {

    /** 命中率告警阈值（&lt; 80% 触发 WARN）。仅在累计样本 &gt; 1000 时评估，避免启动初期误报。 */
    private static final double HIT_RATE_ALERT_THRESHOLD = 0.80;

    /** 命中率告警最低样本量 */
    private static final long HIT_RATE_MIN_SAMPLES = 1_000L;

    /** 内存告警阈值（400MB，为 512MB maxmemory 留出 22% 缓冲） */
    private static final long MEMORY_ALERT_BYTES = 400L * 1024 * 1024;

    @Autowired(required = false)
    private RedisConnectionFactory connectionFactory;

    /**
     * 每分钟采样一次 Redis 运行指标。
     * <p>
     * 启动后延迟 30 秒再开始，避免与 {@link RedisConfig#probeRedisOnStartup()} 启动探测
     * 在同一时间窗口抢占 Redis 连接。
     */
    @Scheduled(fixedRate = 60_000L, initialDelay = 30_000L)
    public void reportVitalMetrics() {
        if (connectionFactory == null) {
            return;  // Redis 未配置：静默跳过
        }

        RedisConnection conn = null;
        try {
            conn = connectionFactory.getConnection();
            Properties stats = conn.info("stats");
            Properties memory = conn.info("memory");
            Properties clients = conn.info("clients");
            Long dbSize = conn.dbSize();

            if (stats == null || memory == null || clients == null) {
                log.debug("Redis INFO 返回空，跳过本轮采样");
                return;
            }

            long hits = parseLongSafe(stats.getProperty("keyspace_hits"));
            long misses = parseLongSafe(stats.getProperty("keyspace_misses"));
            long totalSamples = hits + misses;
            double hitRate = totalSamples == 0 ? 1.0 : (double) hits / totalSamples;

            long memBytes = parseLongSafe(memory.getProperty("used_memory"));
            String memHuman = nullSafe(memory.getProperty("used_memory_human"), "?");
            String connectedClients = nullSafe(clients.getProperty("connected_clients"), "?");

            log.info("Redis 指标 | dbSize={} | mem={} | clients={} | hits={}/misses={} | hitRate={}%",
                    dbSize, memHuman, connectedClients,
                    hits, misses,
                    String.format("%.2f", hitRate * 100));

            // 命中率告警（样本量充足时才评估）
            if (totalSamples > HIT_RATE_MIN_SAMPLES && hitRate < HIT_RATE_ALERT_THRESHOLD) {
                log.warn("⚠️ Redis 命中率 {}% 低于阈值 {}%，样本 {} —— 请检查缓存 Key 设计是否合理",
                        String.format("%.2f", hitRate * 100),
                        String.format("%.0f", HIT_RATE_ALERT_THRESHOLD * 100),
                        totalSamples);
            }

            // 内存告警
            if (memBytes > MEMORY_ALERT_BYTES) {
                log.warn("⚠️ Redis 内存使用 {}（{} 字节）超过告警阈值 400MB，接近 maxmemory 512MB 上限",
                        memHuman, memBytes);
            }
        } catch (Exception e) {
            // Redis 连接异常 / INFO 解析异常（罕见）：降级为 DEBUG 日志，不影响业务
            log.debug("Redis 指标采样失败（非关键）: {}", e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception ignored) {
                    // 连接关闭异常静默吞掉
                }
            }
        }
    }

    /** 安全解析 Long，解析失败时返回 0 —— 防御 Redis 不同版本 INFO 字段格式差异。 */
    private static long parseLongSafe(String s) {
        if (s == null || s.isEmpty()) return 0L;
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static String nullSafe(String s, String fallback) {
        return s == null || s.isEmpty() ? fallback : s;
    }
}
