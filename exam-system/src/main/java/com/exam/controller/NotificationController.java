package com.exam.controller;

import com.exam.common.constants.NotificationTypeWhitelist;
import com.exam.common.result.PageResult;
import com.exam.common.result.Result;
import com.exam.entity.SysUser;
import com.exam.mapper.SysUserMapper;
import com.exam.security.SecurityUtils;
import com.exam.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    /** 每页条数上限（OWASP API4 无限资源消耗防护） */
    private static final int MAX_PAGE_SIZE = 50;
    /** 页码上限（深度分页防 DoS） */
    private static final int MAX_PAGE_NUMBER = 10000;
    /** 批量操作单次最大条数（OWASP API4 防护） */
    private static final int MAX_BATCH_SIZE = 100;

    @Autowired private NotificationService notificationService;
    @Autowired private SysUserMapper userMapper;

    /**
     * 通知列表查询（分页 + 可选筛选）
     *
     * @param page   页码（默认 1，上限 10000）
     * @param size   每页条数（默认 10，上限 50）
     * @param type   通知类型筛选（可选）—— 仅接受 {@link NotificationTypeWhitelist} 白名单内的值，
     *               其他值被忽略为 null，防止 SQL 猜测和脏数据污染
     * @param isRead 已读状态筛选（可选）—— 仅接受 0（未读）/ 1（已读），其他值被忽略为 null
     */
    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer isRead) {
        Long userId = SecurityUtils.getCurrentUserId();

        // size 边界保护（OWASP API4）
        if (size == null || size < 1) size = 10;
        if (size > MAX_PAGE_SIZE) size = MAX_PAGE_SIZE;
        if (page == null || page < 1) page = 1;
        if (page > MAX_PAGE_NUMBER) page = MAX_PAGE_NUMBER;

        // type 白名单（防 SQL 猜测 + 数据污染）
        if (type != null && !NotificationTypeWhitelist.isValid(type)) {
            type = null;
        }

        // isRead 取值限定 0/1
        if (isRead != null && isRead != 0 && isRead != 1) {
            isRead = null;
        }

        return Result.success(notificationService.listNotifications(userId, page, size, type, isRead));
    }

    @GetMapping("/unread-count")
    public Result<Map<String, Object>> unreadCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(notificationService.getUnreadCount(userId));
    }

    @PutMapping("/read/{id}")
    public Result<Void> markRead(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        notificationService.markAsRead(id, userId);
        return Result.success("已标记已读", null);
    }

    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        Long userId = SecurityUtils.getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return Result.success("已全部标记已读", null);
    }

    /** 删除单条通知（仅限本人） */
    @DeleteMapping("/{id}")
    public Result<Integer> delete(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        int deleted = notificationService.deleteNotification(id, userId);
        return Result.success("已删除", deleted);
    }

    /**
     * 批量删除通知（OWASP API4 防护：单次最多 {@value #MAX_BATCH_SIZE} 条）
     */
    @DeleteMapping("/batch")
    public Result<Integer> batchDelete(@RequestBody Map<String, List<Long>> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Long> ids = body == null ? null : body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return Result.success("无待删除项", 0);
        }
        if (ids.size() > MAX_BATCH_SIZE) {
            return Result.error(400, "单次批量操作不能超过 " + MAX_BATCH_SIZE + " 条");
        }
        int deleted = notificationService.batchDeleteNotifications(ids, userId);
        return Result.success("已删除 " + deleted + " 条", deleted);
    }

    /**
     * 批量标记已读（OWASP API4 防护：单次最多 {@value #MAX_BATCH_SIZE} 条）
     */
    @PutMapping("/batch-read")
    public Result<Integer> batchMarkRead(@RequestBody Map<String, List<Long>> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Long> ids = body == null ? null : body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return Result.success("无待处理项", 0);
        }
        if (ids.size() > MAX_BATCH_SIZE) {
            return Result.error(400, "单次批量操作不能超过 " + MAX_BATCH_SIZE + " 条");
        }
        int updated = notificationService.batchMarkAsRead(ids, userId);
        return Result.success("已标记 " + updated + " 条", updated);
    }

    @GetMapping("/pending")
    public Result<List<Map<String, Object>>> pending() {
        Long userId = SecurityUtils.getCurrentUserId();
        SysUser user = userMapper.selectById(userId);
        if (user == null) return Result.success(List.of());
        return Result.success(notificationService.getPendingItems(userId, user.getRoleId(), user.getClassId()));
    }
}
