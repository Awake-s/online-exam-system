package com.exam.perf;

import com.exam.perf.support.ChineseFakerHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 性能压测造数据测试抽象基类。
 *
 * <p>关键约束：
 * <ul>
 *   <li>必须使用 {@code @ActiveProfiles("perf")} 切换到压测专用配置（数据库切到 perf 库，Redis 切到 db1）</li>
 *   <li>使用 {@code TestInstance.Lifecycle.PER_CLASS} 保证 Faker 实例在整个测试类内复用</li>
 *   <li>统一日志输出格式，方便定位每个 step 的执行情况</li>
 * </ul>
 *
 * <p>子类继承本基类后无需再次声明 {@code @SpringBootTest}/{@code @ActiveProfiles}。
 *
 * @author 性能压测工具链 v2.0
 */
@SpringBootTest
@ActiveProfiles("perf")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BasePerfTest {

    /** 中文数据生成器（PER_CLASS 生命周期，整个测试类内复用） */
    protected ChineseFakerHelper faker;

    /** 当前测试类内全局开始时间，用于统计各 step 耗时 */
    protected long classStartMillis;

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss");

    @BeforeAll
    void initFakerOnce() {
        this.faker = new ChineseFakerHelper();
        this.classStartMillis = System.currentTimeMillis();
        log("====================================================");
        log("⚡ Perf Data Tool v2.0 启动");
        log("📦 测试类：" + this.getClass().getSimpleName());
        log("🌱 数据规模：" + com.exam.perf.support.PerfDataConstants.getScaleLabel());
        log("====================================================");
    }

    @BeforeEach
    void initSecurityContext() {
        // 默认登录为 admin（id=1，是项目初始化的管理员）
        // 各 step 内部可调用 PerfSecurityHelper 重新登录为其它角色
        com.exam.perf.support.PerfSecurityHelper.loginAsAdmin(1L);
    }

    /**
     * 统一日志输出（带时间戳 + 类名前缀）
     */
    protected void log(String message) {
        System.out.println("[" + LocalDateTime.now().format(TS) + "] [" + this.getClass().getSimpleName() + "] " + message);
    }

    /**
     * 输出 step 起始日志，并返回当前毫秒时间戳（用于结束时计算耗时）
     */
    protected long stepBegin(String stepName) {
        long now = System.currentTimeMillis();
        log("▶ START " + stepName);
        return now;
    }

    /**
     * 输出 step 结束日志，自动计算耗时
     */
    protected void stepEnd(String stepName, long beginMillis, int recordsAffected) {
        long elapsed = System.currentTimeMillis() - beginMillis;
        log("✅ DONE  " + stepName + " | 影响行数 = " + recordsAffected + " | 耗时 = " + elapsed + " ms");
    }
}
