package com.exam.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Properties;

/**
 * Redis 配置类。
 * <p>
 * <b>业务目的</b>：
 * <ol>
 *   <li>提供类型安全的 {@link RedisTemplate}（键用 {@code StringRedisSerializer}，值用
 *       {@code Jackson2JsonRedisSerializer}），替代 Spring Boot 默认的
 *       {@code JdkSerializationRedisSerializer}（它会在 Value 前加二进制字节前缀导致不可读）。
 *       现有 6 类业务全部使用 {@link StringRedisTemplate}，此 Bean 为未来存对象场景预留。</li>
 *   <li>应用启动完成后<b>探测 Redis 连通性</b>并打印版本、模式、Uptime、Key 数等关键指标，
 *       便于部署环境排障（缺失此功能时，Redis 连不上会静默降级到内存 fallback，很难发现）。</li>
 *   <li>与 Spring Boot Actuator 的 {@code /actuator/health} 协同，让运维可实时监控 Redis 状态。</li>
 * </ol>
 * <p>
 * <b>设计原则</b>：
 * <ul>
 *   <li>即使 Redis 连接失败也不阻断应用启动（现有业务代码已用 {@code @Autowired(required = false)} 容错）。</li>
 *   <li>启动探测只打印日志，不抛异常，不影响业务可用性。</li>
 * </ul>
 * <p>
 * <b>权威参考</b>：
 * <ul>
 *   <li><a href="https://www.baeldung-cn.com/spring-data-redis-properties">Baeldung · Spring Data Redis 基于属性的配置</a></li>
 *   <li><a href="https://www.cnblogs.com/jason207010/p/18215440">Spring Boot 2.7.18 + Lettuce 生产级调优</a></li>
 *   <li>阿里巴巴 Java 开发手册 §4.1：外部依赖配置必须显式化</li>
 * </ul>
 *
 * @author Cascade
 * @since Redis 权威方案 P1-2
 */
@Slf4j
@Configuration
public class RedisConfig {

    // ▼ 双命名空间回退语法：优先读取新命名空间 spring.data.redis.*（Spring Boot 2.4+ 官方推荐），
    //   不存在时回退到旧命名空间 spring.redis.*（Spring Boot 2.7.18 兼容），
    //   都不存在时使用最终默认值。确保 yaml 配置迁移期间零破坏。
    //   参考 https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html#application-properties.data
    @Value("${spring.data.redis.host:${spring.redis.host:unknown}}")
    private String host;

    @Value("${spring.data.redis.port:${spring.redis.port:0}}")
    private int port;

    @Value("${spring.data.redis.database:${spring.redis.database:0}}")
    private int database;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 自定义 {@code RedisTemplate<String, Object>} Bean。
     * <p>
     * 默认 Spring Boot 自动配置的 {@code RedisTemplate<Object, Object>} 使用
     * {@code JdkSerializationRedisSerializer}，存入对象时会带上二进制字节前缀
     * （如 {@code \xac\xed\x00\x05t\x00}），通过 {@code redis-cli} 查看时不可读。
     * <p>
     * 本 Bean 统一：Key = String，Value = JSON（Jackson），便于排障和跨语言互通。
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // Value 序列化器：Jackson JSON（支持多态类型）
        Jackson2JsonRedisSerializer<Object> jsonSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(
                mapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );
        jsonSerializer.setObjectMapper(mapper);

        // Key 序列化器：String
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 应用启动完成后探测 Redis 连通性，打印关键运行指标。
     * <p>
     * 若连接失败：只打 WARN 日志，不阻断启动（所有 Redis 业务代码都有降级路径）。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void probeRedisOnStartup() {
        if (stringRedisTemplate == null) {
            log.warn("╔══════════════════════════════════════════════════════════════╗");
            log.warn("║  Redis 未配置（StringRedisTemplate 为 null）                 ║");
            log.warn("║  状态：全部 Redis 业务将降级到内存 fallback 或直通 DB        ║");
            log.warn("╚══════════════════════════════════════════════════════════════╝");
            return;
        }

        RedisConnectionFactory factory = stringRedisTemplate.getConnectionFactory();
        if (factory == null) {
            log.warn("Redis ConnectionFactory 为 null，跳过启动探测");
            return;
        }

        try (RedisConnection conn = factory.getConnection()) {
            Properties info = conn.info("server");
            Long dbSize = conn.dbSize();
            String version = info != null ? info.getProperty("redis_version") : "unknown";
            String mode = info != null ? info.getProperty("redis_mode") : "unknown";
            String uptime = info != null ? info.getProperty("uptime_in_seconds") : "unknown";

            log.info("╔══════════════════════════════════════════════════════════════╗");
            log.info("║                  Redis 连接成功                              ║");
            log.info("╠══════════════════════════════════════════════════════════════╣");
            log.info("║  Host    : {}:{}  (db={})", host, port, database);
            log.info("║  Version : {}", version);
            log.info("║  Mode    : {}", mode);
            log.info("║  Uptime  : {}s", uptime);
            log.info("║  DB Size : {} keys", dbSize);
            log.info("║  业务覆盖: JWT黑名单 / 消息幂等 / 权限缓存 / Typing限流 /    ║");
            log.info("║           撤回草稿 / 通知去重 （共 6 类）                    ║");
            log.info("╚══════════════════════════════════════════════════════════════╝");

            // 老版本预警
            if (version != null && version.startsWith("3.")) {
                log.warn("⚠️  检测到 Redis {}（老版本，2016 年发布）", version);
                log.warn("   建议升级到 Redis 7.x 以获得 ACL / Streams / 多线程 IO 等新特性");
                log.warn("   详见：doc/技术栈分析/Redis在线考试系统深度使用分析与权威方案.md §P2-1");
            }
        } catch (Exception e) {
            log.warn("╔══════════════════════════════════════════════════════════════╗");
            log.warn("║                  Redis 连接失败（非致命）                    ║");
            log.warn("╠══════════════════════════════════════════════════════════════╣");
            log.warn("║  Host  : {}:{}", host, port);
            log.warn("║  Error : {}", e.getMessage());
            log.warn("║  状态  : 业务降级到内存 fallback（TokenBlacklist / RecallDraft）");
            log.warn("║          或直通 DB（Idempotency / Permission / Dedup）       ║");
            log.warn("╚══════════════════════════════════════════════════════════════╝");
        }
    }
}
