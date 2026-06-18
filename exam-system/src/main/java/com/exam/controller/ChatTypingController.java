package com.exam.controller;

import com.exam.common.constants.ChatConstants;
import com.exam.service.ChatPermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * L3-M0-6：Typing Indicator —— 实时"对方正在输入..."提示。
 * <p>
 * <b>业界参考</b>：
 * <ul>
 *   <li>WhatsApp — "Typing..."事件驱动通信，一次性轻量事件，不走消息持久化路径</li>
 *   <li>Telegram / Signal / iMessage / WeChat / QQ — 均有类似实现</li>
 *   <li>参考 <a href="https://parashar--manas.medium.com/how-whatsapps-typing-indicator-actually-works-8a3cf18f2bad">Manas, "How WhatsApp's Typing Indicator Actually Works" (2026)</a></li>
 * </ul>
 *
 * <h3>架构设计</h3>
 * <ul>
 *   <li><b>入站通道</b>：STOMP {@code /app/chat/typing}（客户端 → 服务端的首个 @MessageMapping 端点）</li>
 *   <li><b>出站通道</b>：{@code /user/{receiverId}/queue/typing-events} 定向推送</li>
 *   <li><b>事件载荷</b>：{@code {type: TYPING_START|TYPING_STOP, senderId, timestamp}}</li>
 *   <li><b>不持久化</b>：disposable event，丢包可接受（前端有 TTL 兜底自动清除）</li>
 * </ul>
 *
 * <h3>安全与防滥用（L3-bugfix TYPING-001 重构）</h3>
 * <ol>
 *   <li><b>身份校验</b>：Principal 由 {@code CustomHandshakeHandler} 注入，未登录直接静默忽略</li>
 *   <li><b>聊天权限校验（单一事实源）</b>：委托 {@link ChatPermissionService#canChatSilent}，
 *       与 {@code sendMessage} / {@code sendMessagesBatch} 共享同一套角色矩阵 + 考试中防作弊
 *       规则；typing 于 sendMessage 的权限口径永不漂移。Redis 30s 缓存避免高频 typing 打穿库。
 *       <b>根治零会话首次 typing 失灵</b>——旧版用 {@code chat_conversation} 表存在性做权限代理。
 *       对标 Microsoft Teams Education / Google Chat Workspace 业界实践。</li>
 *   <li><b>速率限制</b>：每对 sender→receiver 每秒最多 {@value ChatConstants#TYPING_RATE_LIMIT_PER_SECOND} 个事件。
 *       Redis INCR + 1s EXPIRE；故障降级为不限流（低危）。</li>
 *   <li><b>不给自己发</b>：senderId == receiverId 静默忽略（{@code canChatSilent} 内部也会拦截，此处只是提前回避缓存穿透）。</li>
 * </ol>
 *
 * <h3>容错</h3>
 * 端点签名返回 void 且捕获所有异常 —— typing 是"锦上添花"的特性，任何故障都不能污染主消息流。
 *
 * @author Cascade
 * @since L3-M0-6
 */
@Slf4j
@Controller
public class ChatTypingController {

    @Autowired private SimpMessagingTemplate messagingTemplate;
    // L3-bugfix TYPING-001：权限校验委托给单一事实源 Service，不再直读 conversation 表
    @Autowired private ChatPermissionService permissionService;
    @Autowired(required = false) private StringRedisTemplate redisTemplate;

    /**
     * 处理客户端发来的 typing 事件并转发给对方。
     * <p>
     * 注意：方法声明无返回值 + 内部吞掉所有异常，符合 STOMP @MessageMapping 端点
     * "不可抛出未捕获异常"的约定（否则会断 WS 连接）。
     *
     * @param body      {@code {receiverId: number, type: "TYPING_START"|"TYPING_STOP"}}
     * @param principal 当前已认证用户（{@code StompPrincipal.name} = userId）
     */
    @MessageMapping("/chat/typing")
    public void handleTyping(@Payload Map<String, Object> body, Principal principal) {
        if (principal == null || body == null) return;

        Long senderId;
        Long receiverId;
        String type;
        try {
            senderId = Long.parseLong(principal.getName());
            Object recvObj = body.get("receiverId");
            Object typeObj = body.get("type");
            if (recvObj == null || typeObj == null) return;
            receiverId = Long.parseLong(String.valueOf(recvObj));
            type = String.valueOf(typeObj);
        } catch (Exception e) {
            // 格式错误 / 非数字 → 静默忽略（disposable event 不回报）
            return;
        }

        // 白名单事件类型
        if (!"TYPING_START".equals(type) && !"TYPING_STOP".equals(type)) {
            return;
        }

        // 禁止自推（理论上前端不会发，防御性编程 + 避免缓存穿透）
        if (senderId.equals(receiverId)) return;

        // ====== L3-bugfix TYPING-001：聊天权限单一事实源校验 ======
        // 工业级业界对标：Microsoft Teams Education / Google Chat Workspace
        // 改造前：用 chat_conversation 表存在性做权限代理 → 零会话首次 typing 丢失（bug）
        // 改造后：调用 canChatSilent（与 sendMessage 共享的角色矩阵 + 考试中防作弊）+ Redis 30s 缓存
        // - 鼋前首次 typing 立即生效（对齐 Telegram/WhatsApp/Teams 的 UX）
        // - typing 与 sendMessage 权限永不漂移（治本保证）
        // - 高频 typing 事件 Redis 缓存命中，不打穿 DB（3s 心跳 × 30s TTL = 每对 ~10 次/分钟 DB 命中）
        // - 权限拒绝或异常 → 立即静默返回，typing 不会流入 → 避免成为权限探测工具
        if (!permissionService.canChatSilent(senderId, receiverId)) {
            return;
        }

        // 速率限制（每对 sender→receiver）
        if (isRateLimited(senderId, receiverId)) {
            log.trace("typing 事件速率限制触发: sender={}, receiver={}", senderId, receiverId);
            return;
        }

        // 组装并转发
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", type);
        event.put("senderId", senderId);
        event.put("timestamp", System.currentTimeMillis());

        try {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(receiverId),
                    "/queue/typing-events",
                    event);
        } catch (Exception e) {
            // 接收方不在线 / WS 故障 → typing 事件允许丢失
            log.debug("转发 typing 事件失败（非关键路径）: receiver={}, err={}", receiverId, e.getMessage());
        }
    }

    /**
     * 每对 sender→receiver 每秒最多 N 个 typing 事件（START+STOP 共计）。
     * <p>
     * Redis 不可用时返回 false（不限流）—— typing 不是安全敏感路径。
     */
    private boolean isRateLimited(Long senderId, Long receiverId) {
        if (redisTemplate == null) return false;
        String key = ChatConstants.TYPING_RATE_KEY_PREFIX + senderId + ":" + receiverId;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, 1, TimeUnit.SECONDS);
            }
            return count != null && count > ChatConstants.TYPING_RATE_LIMIT_PER_SECOND;
        } catch (Exception e) {
            log.debug("typing 速率限制 Redis 故障，降级为不限流: {}", e.getMessage());
            return false;
        }
    }
}
