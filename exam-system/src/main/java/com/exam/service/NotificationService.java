package com.exam.service;

import com.exam.common.result.PageResult;
import java.util.List;
import java.util.Map;

public interface NotificationService {
    /**
     * 给指定用户发送通知（基础版 - 默认优先级普通，无发送者信息）
     */
    void notifyUser(Long userId, String type, String title, String content, String bizType, Long bizId);

    /**
     * 给指定用户发送通知（E1 扩展版 - 支持优先级/发送者/actionUrl 等）
     * @param options 扩展选项（priority/sender/actionUrl/extras），传 {@link NotificationOptions#defaults()} 等价于基础版
     */
    void notifyUser(Long userId, String type, String title, String content, String bizType, Long bizId,
                    NotificationOptions options);

    /**
     * 给指定用户发送通知（带时间窗口去重）。
     * 若最近 dedupeWindowMinutes 分钟内存在同 user+type+bizType+bizId 的通知，
     * 则更新该通知的 title/content/createTime 并重置为未读，避免短时间内刷屏。
     * 适用于"重复发布"场景（如教师修正分数后重新发布成绩）。
     */
    void notifyUserWithDedupe(Long userId, String type, String title, String content,
                              String bizType, Long bizId, int dedupeWindowMinutes);

    /**
     * 给指定用户发送通知（带时间窗口去重 + 扩展选项）
     */
    void notifyUserWithDedupe(Long userId, String type, String title, String content,
                              String bizType, Long bizId, int dedupeWindowMinutes,
                              NotificationOptions options);

    /** 给班级所有学生发送通知（批量） */
    void notifyClassStudents(Long classId, String type, String title, String content, String bizType, Long bizId);

    /**
     * 给班级所有学生发送通知（E1 扩展版）
     */
    void notifyClassStudents(Long classId, String type, String title, String content, String bizType, Long bizId,
                             NotificationOptions options);

    /** 给所有管理员发送通知 */
    void notifyAdmins(String type, String title, String content, String bizType, Long bizId);

    /**
     * 给所有管理员发送通知（E1 扩展版）
     */
    void notifyAdmins(String type, String title, String content, String bizType, Long bizId,
                      NotificationOptions options);

    /**
     * 获取当前用户的通知列表（分页 + 可选筛选）
     * @param userId 当前用户 ID
     * @param page 页码（>= 1）
     * @param size 每页条数（建议 Controller 层限制上限防 DoS）
     * @param type 通知类型筛选（可选），null 表示不筛选。Controller 层应通过白名单校验。
     * @param isRead 已读状态筛选（可选），0=未读 / 1=已读 / null=不筛选
     */
    PageResult<Map<String, Object>> listNotifications(Long userId, Integer page, Integer size,
                                                       String type, Integer isRead);

    /** 获取未读通知数量 */
    Map<String, Object> getUnreadCount(Long userId);

    /** 标记单条通知已读 */
    void markAsRead(Long notificationId, Long userId);

    /** 全部标记已读 */
    void markAllAsRead(Long userId);

    /**
     * 删除单条通知（仅限当前用户所有）
     * @param notificationId 通知 ID
     * @param userId 当前用户 ID（用于权限最小化校验，防止越权）
     * @return 实际删除的行数（0 表示不存在或无权限）
     */
    int deleteNotification(Long notificationId, Long userId);

    /**
     * 批量删除通知（仅限当前用户所有）
     * @param notificationIds 通知 ID 列表
     * @param userId 当前用户 ID（权限最小化校验）
     * @return 实际删除的行数
     */
    int batchDeleteNotifications(List<Long> notificationIds, Long userId);

    /**
     * 批量标记已读（仅限当前用户所有）
     * @param notificationIds 通知 ID 列表
     * @param userId 当前用户 ID（权限最小化校验）
     * @return 实际更新的行数
     */
    int batchMarkAsRead(List<Long> notificationIds, Long userId);

    /** 获取待办事项（从业务表实时聚合） */
    List<Map<String, Object>> getPendingItems(Long userId, Long roleId, Long classId);
}
