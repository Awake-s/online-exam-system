package com.exam.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 通知推送服务 - WebSocket 实时推送
 */
@Slf4j
@Service
public class NotificationPushService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * 推送通知给指定用户（点对点）
     * @param userId 用户ID
     * @param notification 通知数据
     */
    public void pushToUser(Long userId, Map<String, Object> notification) {
        try {
            // 目标地址：/user/{userId}/queue/notification
            messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notification",
                notification
            );
            log.debug("✅ 推送通知给用户 {}: {}", userId, notification.get("title"));
        } catch (Exception e) {
            log.error("❌ 推送通知失败，用户ID: {}", userId, e);
        }
    }

    /**
     * 推送通知给班级所有在线学生（广播）
     * @param classId 班级ID
     * @param notification 通知数据
     */
    public void pushToClass(Long classId, Map<String, Object> notification) {
        try {
            // 目标地址：/topic/class/{classId}/notification
            messagingTemplate.convertAndSend(
                "/topic/class/" + classId + "/notification",
                notification
            );
            log.debug("✅ 推送通知给班级 {}: {}", classId, notification.get("title"));
        } catch (Exception e) {
            log.error("❌ 推送通知失败，班级ID: {}", classId, e);
        }
    }

    /**
     * 推送通知给所有在线管理员（广播）
     * @param notification 通知数据
     */
    public void pushToAdmins(Map<String, Object> notification) {
        try {
            // 目标地址：/topic/admin/notification
            messagingTemplate.convertAndSend(
                "/topic/admin/notification",
                notification
            );
            log.debug("✅ 推送通知给所有管理员: {}", notification.get("title"));
        } catch (Exception e) {
            log.error("❌ 推送通知失败（管理员）", e);
        }
    }
}
