package com.exam.common.constants;

import java.util.Set;

/**
 * 通知类型白名单 - 用于 API 查询参数校验
 * <p>
 * 主要作用：
 * 1. 防止 SQL 猜测攻击：前端传入未知 type 时直接忽略
 * 2. 防止脏数据污染查询：仅接受系统已知类型
 * 3. 新增通知类型时必须在此处同步注册，形成白名单闭环
 * <p>
 * 与 NotificationServiceImpl 中实际使用的 type 枚举保持一致。
 */
public final class NotificationTypeWhitelist {

    private static final Set<String> ALLOWED = Set.of(
            // 考试相关
            "EXAM_PUBLISHED", "EXAM_UPDATED", "EXAM_CANCELLED",
            "EXAM_SUBMITTED", "EXAM_AUTO_SUBMITTED", "EXAM_ABSENT",
            "EXAM_END_SUMMARY", "EXAM_CREATED",
            // 成绩相关（SCORE_UPDATED：教师修正后重新发布成绩，对齐 MarkingServiceImpl）
            "SCORE_PUBLISHED", "SCORE_UPDATED",
            // 账号相关
            "ACCOUNT_CREATED", "USER_CREATED"
    );

    /**
     * 校验 type 是否为已知白名单类型
     * @param type 前端传入的通知类型
     * @return true 表示白名单内，可安全用于查询；false 应视为无效过滤条件
     */
    public static boolean isValid(String type) {
        return type != null && ALLOWED.contains(type);
    }

    private NotificationTypeWhitelist() {}
}
