package com.exam.config;

import com.exam.service.ChatVisibilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import javax.annotation.PreDestroy;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket 在线状态管理器
 *
 * 设计要点（对标业界最佳实践）：
 * 1. 通过 event.getUser() 获取 Principal（由 CustomHandshakeHandler 设置）
 *    注意：SessionConnectedEvent.getMessage() 包装的是 CONNECTED 帧，其中不包含 sessionAttributes，
 *    因此必须通过 event.getUser() 而非 accessor.getSessionAttributes() 获取用户标识
 * 2. 基于会话 ID 追踪：用户可能同时拥有多个 WebSocket 会话（聊天 /ws + 通知 /ws/notification），
 *    只有当所有会话都断开时才判定用户离线，避免单端点断开错误标记离线
 * 3. 延迟离线广播：用户断开后等待 5 秒再广播离线，防止浏览器刷新导致"在线→离线→在线"闪烁
 * 4. 幂等处理：SessionDisconnectEvent 可能多次触发（Spring 文档明确说明），基于 sessionId 的 Set 移除天然幂等
 * 5. 通过 /user/queue/online-status 定向推送状态变化（M4 修复）：
 *    按业务可见性规则（{@link ChatVisibilityService#getStatusWatchers}）逐人推送，
 *    替换原 /topic/online-status 全局广播，避免用户枚举 + 行为追踪
 */
@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    /** 离线广播延迟时间（秒），防止刷新页面导致短暂离线闪烁 */
    private static final int OFFLINE_DELAY_SECONDS = 5;

    /**
     * userId → sessionId 集合，追踪每个用户的所有活跃 WebSocket 会话。
     * 用户可能同时通过 /ws（聊天）和 /ws/notification（通知）建立多个会话，
     * 只有当所有会话都断开后才判定用户离线。
     */
    private final Map<String, Set<String>> userSessions = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ws-status-scheduler");
        t.setDaemon(true);
        return t;
    });

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ChatVisibilityService visibilityService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        // 关键：必须通过 event.getUser() 获取 Principal
        // SessionConnectedEvent 的 message 是 CONNECTED 帧，不含 sessionAttributes
        Principal principal = event.getUser();
        if (principal == null) {
            log.warn("WebSocket 连接事件中 Principal 为 null，无法追踪在线状态");
            return;
        }
        String userId = principal.getName();

        // 从 CONNECTED 帧消息头中提取 sessionId
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = sha.getSessionId();
        if (sessionId == null) {
            log.warn("WebSocket 连接事件中 sessionId 为 null，userId={}", userId);
            return;
        }

        Set<String> sessions = userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet());
        sessions.add(sessionId);

        // 只有第一个会话建立时才广播上线
        if (sessions.size() == 1) {
            log.info("WebSocket 用户上线: userId={}, sessionId={}", userId, sessionId);
            broadcastStatus(userId, true);
        } else {
            log.info("WebSocket 用户新增会话: userId={}, sessionId={}, 当前会话数={}", userId, sessionId, sessions.size());
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        // 同样通过 event.getUser() 获取 Principal
        Principal principal = event.getUser();
        if (principal == null) {
            log.warn("WebSocket 断开事件中 Principal 为 null，无法追踪离线状态");
            return;
        }
        String userId = principal.getName();
        String sessionId = event.getSessionId();
        log.info("WebSocket 用户断开连接: userId={}, sessionId={}", userId, sessionId);

        // 延迟移除会话，防止刷新页面导致短暂离线闪烁
        // 每个会话独立调度，基于 sessionId 的 Set.remove() 天然幂等，无需额外去重
        scheduler.schedule(() -> {
            Set<String> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                    log.info("WebSocket 用户离线（延迟确认）: userId={}", userId);
                    broadcastStatus(userId, false);
                } else {
                    log.info("WebSocket 用户关闭一个会话，剩余 {} 个: userId={}", sessions.size(), userId);
                }
            }
        }, OFFLINE_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 按业务可见性规则定向推送在线状态（修复 M4：用户枚举 + 行为追踪漏洞）。
     * <p>
     * 推送目标：{@link ChatVisibilityService#getStatusWatchers(Long)} 返回的观察者列表，
     * 每人通过 {@code /user/{id}/queue/online-status} 独立 queue 接收。
     * <p>
     * 安全影响：
     * <ul>
     *   <li>学生不再能订阅到教师/其他学生的上下线时间戳</li>
     *   <li>防止通过订阅 /topic/online-status 进行全系统用户枚举</li>
     * </ul>
     */
    private void broadcastStatus(String userId, boolean online) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("online", online);

        Long uid;
        try {
            uid = Long.parseLong(userId);
        } catch (NumberFormatException e) {
            log.warn("在线状态 userId 格式异常: {}", userId);
            return;
        }

        Set<Long> watchers = visibilityService.getStatusWatchers(uid);
        for (Long watcherId : watchers) {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(watcherId),
                    "/queue/online-status",
                    payload);
        }
        log.info("定向推送在线状态: userId={}, online={}, watchers={}", userId, online, watchers.size());
    }

    public Set<String> getOnlineUsers() {
        return userSessions.keySet();
    }

    public boolean isUserOnline(Long userId) {
        Set<String> sessions = userSessions.get(String.valueOf(userId));
        return sessions != null && !sessions.isEmpty();
    }

    @PreDestroy
    public void destroy() {
        log.info("关闭 WebSocket 在线状态调度器");
        scheduler.shutdownNow();
    }
}
