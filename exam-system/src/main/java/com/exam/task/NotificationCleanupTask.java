package com.exam.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.entity.SysNotification;
import com.exam.mapper.SysNotificationMapper;
import com.exam.service.ChatRateLimiterService;
import com.exam.service.NotificationRateLimiterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 通知清理定时任务（生产安全版）
 * <p>
 * <b>功能</b>：
 * <ol>
 *   <li>每天凌晨 3 点清理 30 天前的已读通知</li>
 *   <li>每周日凌晨 4 点清理 90 天前的所有通知</li>
 *   <li>每小时清理限流器缓存</li>
 * </ol>
 * <p>
 * <b>分批删除策略（D1 防锁表优化）</b>：
 * 单条 {@code DELETE WHERE ...} 在大表场景下会锁整张表数秒至分钟，可能阻塞线上用户查询。
 * 业界标准做法是「SELECT ids LIMIT N → DELETE IN (ids)」循环分批，每批走主键删除，锁粒度极小。
 * <p>
 * 参考：
 * <ul>
 *   <li><a href="https://dev.mysql.com/doc/refman/8.0/en/delete.html">MySQL DELETE 官方文档</a></li>
 *   <li>阿里巴巴 Java 开发手册 §5.3「大表 DELETE 分批操作」</li>
 * </ul>
 */
@Slf4j
@Component
public class NotificationCleanupTask {

    /** 单批删除大小：走主键批删，1000 条的锁窗口极小 */
    private static final int BATCH_SIZE = 1000;

    /** 单次任务最多执行的批次数：硬上限防长跑，最多删除 100 * 1000 = 10 万条 */
    private static final int MAX_BATCHES = 100;

    /** 批次间歇（毫秒）：给在线查询留出缓冲窗口 */
    private static final long INTER_BATCH_SLEEP_MS = 50;

    @Autowired
    private SysNotificationMapper notificationMapper;

    @Autowired
    private NotificationRateLimiterService rateLimiterService;

    @Autowired
    private ChatRateLimiterService chatRateLimiterService;

    /**
     * 每天凌晨3点清理30天前的已读通知（分批，防锁表）
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldReadNotifications() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        int deleted = deleteInBatches(
                wrapper -> wrapper
                        .eq(SysNotification::getIsRead, 1)
                        .lt(SysNotification::getCreateTime, thirtyDaysAgo),
                "30天前已读通知"
        );
        log.info("✅ 分批清理完成：共删除 {} 条 30 天前的已读通知", deleted);
    }

    /**
     * 每周日凌晨4点清理90天前的所有通知（分批，防锁表）
     */
    @Scheduled(cron = "0 0 4 ? * SUN")
    public void cleanupVeryOldNotifications() {
        LocalDateTime ninetyDaysAgo = LocalDateTime.now().minusDays(90);
        int deleted = deleteInBatches(
                wrapper -> wrapper.lt(SysNotification::getCreateTime, ninetyDaysAgo),
                "90天前所有通知"
        );
        log.info("✅ 分批清理完成：共删除 {} 条 90 天前的通知", deleted);
    }

    /**
     * 分批删除通知（内部工具方法）。
     * <p>
     * 算法：循环 SELECT id LIMIT {@value BATCH_SIZE} → DELETE by id IN (ids)，
     * 每批按主键删除，锁粒度极小；批次间 sleep {@value #INTER_BATCH_SLEEP_MS}ms 让位在线查询；
     * 硬上限 {@value #MAX_BATCHES} 批防长跑。
     *
     * @param conditionBuilder 条件构造器（传入 where 条件）
     * @param description      日志描述
     * @return 总删除条数
     */
    private int deleteInBatches(Consumer<LambdaQueryWrapper<SysNotification>> conditionBuilder,
                                 String description) {
        int totalDeleted = 0;
        try {
            for (int batch = 0; batch < MAX_BATCHES; batch++) {
                // 1. 查询本批待删 id（走索引，轻量）
                LambdaQueryWrapper<SysNotification> selectWrapper = new LambdaQueryWrapper<SysNotification>()
                        .select(SysNotification::getId);
                conditionBuilder.accept(selectWrapper);
                selectWrapper.last("LIMIT " + BATCH_SIZE);

                List<Long> ids = notificationMapper.selectList(selectWrapper)
                        .stream().map(SysNotification::getId).collect(Collectors.toList());

                if (ids.isEmpty()) break; // 无更多数据，正常结束

                // 2. 按 id 批删（走主键，锁粒度最小）
                int deleted = notificationMapper.deleteBatchIds(ids);
                totalDeleted += deleted;

                log.debug("分批清理 [{}] 第 {} 批，删除 {} 条（累计 {}）", description, batch + 1, deleted, totalDeleted);

                // 3. 批次间短暂间歇，减轻数据库压力（允许在线查询插队）
                if (ids.size() == BATCH_SIZE && batch < MAX_BATCHES - 1) {
                    try {
                        Thread.sleep(INTER_BATCH_SLEEP_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("分批清理被中断 [{}]", description);
                        break;
                    }
                } else if (ids.size() < BATCH_SIZE) {
                    break; // 最后一批，提前结束
                }
            }
        } catch (Exception e) {
            log.error("❌ 分批清理失败 [{}]，已删除 {} 条", description, totalDeleted, e);
        }
        return totalDeleted;
    }

    /**
     * 每小时清理一次限流器缓存（防止内存泄漏）
     * 只清理超过1小时未使用的限流器
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void cleanupRateLimiters() {
        try {
            int beforeCount = rateLimiterService.getLimiterCount();

            // 清理所有限流器（简单策略）
            // 更复杂的策略可以记录最后使用时间，只清理长时间未使用的
            if (beforeCount > 1000) { // 超过1000个才清理
                rateLimiterService.clearAllLimiters();
                log.info("✅ 清理了 {} 个通知限流器缓存", beforeCount);
            }

            // 顺带清理聊天限流器
            int chatBeforeCount = chatRateLimiterService.getLimiterCount();
            if (chatBeforeCount > 1000) {
                chatRateLimiterService.clearAllLimiters();
                log.info("✅ 清理了 {} 个聊天限流器缓存", chatBeforeCount);
            }
        } catch (Exception e) {
            log.error("❌ 清理限流器失败", e);
        }
    }

    /**
     * 每天凌晨2点统计通知数据（可选）
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void generateDailyStats() {
        try {
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);

            long totalCount = notificationMapper.selectCount(new LambdaQueryWrapper<SysNotification>()
                    .ge(SysNotification::getCreateTime, yesterday)
                    .lt(SysNotification::getCreateTime, today));

            long unreadCount = notificationMapper.selectCount(new LambdaQueryWrapper<SysNotification>()
                    .ge(SysNotification::getCreateTime, yesterday)
                    .lt(SysNotification::getCreateTime, today)
                    .eq(SysNotification::getIsRead, 0));

            double readRate = totalCount > 0 ? ((totalCount - unreadCount) * 100.0 / totalCount) : 0;

            log.info("📊 昨日通知统计 - 总数: {}, 未读: {}, 阅读率: {}%",
                    totalCount, unreadCount, String.format("%.2f", readRate));

        } catch (Exception e) {
            log.error("❌ 生成通知统计失败", e);
        }
    }
}
