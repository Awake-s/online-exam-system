package com.exam.controller;

import com.exam.common.constants.ChatConstants;
import com.exam.common.result.PageResult;
import com.exam.common.result.Result;
import com.exam.security.SecurityUtils;
import com.exam.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired private ChatService chatService;
    @Autowired private SimpMessagingTemplate messagingTemplate;
    @Autowired private com.exam.config.WebSocketEventListener wsEventListener;
    @Autowired private com.exam.service.ChatVisibilityService chatVisibilityService;

    @GetMapping("/contacts")
    public Result<List<Map<String, Object>>> getContacts() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(chatService.getContacts(userId));
    }

    /**
     * 获取会话列表。
     * <p>
     * L3-M1-4：支持按归档状态过滤：
     * <ul>
     *   <li>无参 / archived=false：主会话列表（默认，向后兼容）</li>
     *   <li>archived=true：已归档会话列表（Telegram/WhatsApp 模式的独立 Archive 视图）</li>
     * </ul>
     */
    @GetMapping("/conversations")
    public Result<List<Map<String, Object>>> getConversations(
            @RequestParam(required = false, defaultValue = "false") Boolean archived) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(chatService.getConversations(userId, Boolean.TRUE.equals(archived)));
    }

    @GetMapping("/conversations/{id}/messages")
    public Result<PageResult<Map<String, Object>>> getMessages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(chatService.getMessages(id, userId, page, size));
    }

    @PostMapping("/messages")
    public Result<Map<String, Object>> sendMessage(@RequestBody Map<String, Object> body) {
        Long senderId = SecurityUtils.getCurrentUserId();
        Object receiverIdObj = body.get("receiverId");
        if (receiverIdObj == null) return Result.error(400, "接收者ID不能为空");
        Long receiverId;
        try {
            receiverId = Long.valueOf(String.valueOf(receiverIdObj));
        } catch (NumberFormatException e) {
            return Result.error(400, "接收者ID格式错误");
        }
        Object contentObj = body.get("content");
        if (contentObj == null) return Result.error(400, "消息内容不能为空");
        String content = String.valueOf(contentObj);
        // 幂等键（可选）：客户端网络重试时用同一 clientMsgId 保证消息只入库一次
        Object clientMsgIdObj = body.get("clientMsgId");
        String clientMsgId = clientMsgIdObj != null ? String.valueOf(clientMsgIdObj) : null;

        Map<String, Object> msg = chatService.sendMessage(senderId, receiverId, content, clientMsgId);

        // 通过 WebSocket 实时推送给接收方
        messagingTemplate.convertAndSendToUser(
                String.valueOf(receiverId), "/queue/messages", msg);
        // 也推送给发送方（多标签页同步）
        messagingTemplate.convertAndSendToUser(
                String.valueOf(senderId), "/queue/messages", msg);

        return Result.success("发送成功", msg);
    }

    /**
     * L3-M3-1：原子批量发送消息（对齐 Slack chat.postMessage batch / Telegram sendMediaGroup / Discord 单消息多附件）。
     * <p>
     * <b>请求体</b>：
     * <pre>
     * {
     *   "receiverId": 42,
     *   "items": [
     *     { "clientMsgId": "c-1", "content": "[img]https://...[/img]" },
     *     { "clientMsgId": "c-2", "content": "[file:pdf:报告.pdf]https://...[/file]" },
     *     { "clientMsgId": "c-3", "content": "请查收" }
     *   ]
     * }
     * </pre>
     * <b>响应体</b>（HTTP 200 + Result.data）：
     * <pre>
     * {
     *   "results": [
     *     { "clientMsgId": "c-1", "status": "sent",         "message": {...} },
     *     { "clientMsgId": "c-2", "status": "already_sent", "message": {...} },
     *     { "clientMsgId": "c-3", "status": "failed",       "error": "消息内容不能为空" }
     *   ]
     * }
     * </pre>
     * <b>WS 推送</b>：循环对每条 {@code status=sent} 的消息独立推送 {@code /user/{uid}/queue/messages}，
     * 接收方体验与单条发送完全一致（单独显示每条气泡）。{@code already_sent} 不重推（幂等语义）。
     * <p>
     * <b>限流</b>：批次级抛 {@code BusinessException} → 由 GlobalExceptionHandler 转 Result.error(500,...)，
     * 前端通过 message 文案识别"发送过于频繁"后展示重发指引。
     */
    @PostMapping("/messages:batch")
    public Result<Map<String, Object>> sendMessagesBatch(@RequestBody Map<String, Object> body) {
        Long senderId = SecurityUtils.getCurrentUserId();
        Object receiverIdObj = body.get("receiverId");
        if (receiverIdObj == null) return Result.error(400, "接收者ID不能为空");
        Long receiverId;
        try {
            receiverId = Long.valueOf(String.valueOf(receiverIdObj));
        } catch (NumberFormatException e) {
            return Result.error(400, "接收者ID格式错误");
        }

        Object itemsObj = body.get("items");
        if (!(itemsObj instanceof List)) {
            return Result.error(400, "items 必须是数组");
        }
        List<Map<String, String>> items = new java.util.ArrayList<>();
        for (Object o : (List<?>) itemsObj) {
            if (!(o instanceof Map)) {
                return Result.error(400, "items 元素格式错误");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = (Map<String, Object>) o;
            Map<String, String> normalized = new java.util.HashMap<>();
            Object cmid = raw.get("clientMsgId");
            Object content = raw.get("content");
            normalized.put("clientMsgId", cmid != null ? String.valueOf(cmid) : null);
            normalized.put("content", content != null ? String.valueOf(content) : null);
            items.add(normalized);
        }

        // 调用 Service —— 批次级校验 / 限流 / per-item 幂等全部在内部处理
        List<Map<String, Object>> perItemResults = chatService.sendMessagesBatch(senderId, receiverId, items);

        // 对每条成功的消息独立做 WS 推送（接收方 + 发送方多标签页同步）
        // 幂等命中（already_sent）不重推，避免接收方重复看到
        for (Map<String, Object> r : perItemResults) {
            String status = String.valueOf(r.get("status"));
            if (!"sent".equals(status)) continue;
            Object msgObj = r.get("message");
            if (!(msgObj instanceof Map)) continue;
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(receiverId), "/queue/messages", msgObj);
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(senderId), "/queue/messages", msgObj);
        }

        Map<String, Object> respData = new java.util.LinkedHashMap<>();
        respData.put("results", perItemResults);
        return Result.success("批次发送完成", respData);
    }

    @PutMapping("/conversations/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        chatService.markAsRead(id, userId);
        return Result.success("标记成功", null);
    }

    @GetMapping("/unread-count")
    public Result<Map<String, Object>> getUnreadCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(chatService.getUnreadCount(userId));
    }

    /**
     * I2：WebSocket 重连后补拉漏消息。
     * <p>
     * 调用时机：前端 {@code websocket.ts} 的 {@code onReconnect} 回调。
     * 仅返回本人作为 sender/receiver 的消息，服务端自动限 limit 到 [1, 500]。
     *
     * @param sinceMessageId 前端本地已有的最大 message.id，首次可传 null 或 0
     * @param limit          单次上限，默认 200，最大 500
     */
    @GetMapping("/messages/incremental")
    public Result<List<Map<String, Object>>> getIncrementalMessages(
            @RequestParam(required = false) Long sinceMessageId,
            @RequestParam(required = false, defaultValue = "200") Integer limit) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(chatService.getIncrementalMessages(userId, sinceMessageId, limit));
    }

    // ========== L3：消息撤回 / 强删 / 会话隐藏 ==========

    /**
     * L3：发送者撤回自己发出的消息（2 分钟内）。
     * <p>
     * 为兼容部分 HTTP 客户端/代理对 DELETE 请求体的过滤，这里采用 POST /recall 而非 DELETE。
     * 撤回后通过 WS /user/queue/message-events 推送 MESSAGE_DELETED 给双方。
     */
    @PostMapping("/messages/{id}/recall")
    public Result<Void> recallMessage(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        chatService.recallMessage(id, userId);
        return Result.success("已撤回", null);
    }

    /**
     * L3-M0-4：获取撤回消息的"重新编辑"草稿（对齐微信/QQ 2 分钟内可编辑）。
     * <p>
     * <b>用例</b>：用户撤回消息后 2 分钟内，前端气泡占位符旁显示"重新编辑"按钮，
     * 点击触发此接口获取原文并回填输入框。
     * <p>
     * <b>安全</b>：Service 层校验只有原发送者能访问；其他人（含接收者、管理员）均 403。
     * <p>
     * <b>响应语义</b>：
     * <ul>
     *   <li>{@code content != null} → 草稿有效，前端回填</li>
     *   <li>{@code content == null} → 草稿过期（超过 2 分钟）或非文字消息，前端隐藏按钮</li>
     * </ul>
     */
    @GetMapping("/messages/{id}/recall-draft")
    public Result<Map<String, Object>> getRecallDraft(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        String content = chatService.getRecallDraft(id, userId);
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("content", content);
        return Result.success(data);
    }

    /**
     * L3：管理员强制删除指定消息（无时限、写入审计日志）。
     * <p>
     * 权限：仅管理员角色，由 Service 层校验。
     */
    @PostMapping("/messages/{id}/admin-delete")
    public Result<Void> adminDeleteMessage(@PathVariable Long id) {
        Long adminId = SecurityUtils.getCurrentUserId();
        chatService.adminDeleteMessage(id, adminId);
        return Result.success("已删除", null);
    }

    /**
     * L3：当前用户从自己列表隐藏会话（不影响对方）。
     * <p>
     * 收到对方或自己的新消息时隐藏状态会自动解除（参见 sendMessage 实现）。
     */
    @DeleteMapping("/conversations/{id}")
    public Result<Void> hideConversation(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        chatService.hideConversation(id, userId);
        return Result.success("已归档", null);
    }

    /**
     * L3-M0-7：会话置顶（对齐微信 / WhatsApp / Telegram）。
     * <p>
     * HTTP 语义：PUT 幂等。已置顶时再次调用不报错。
     * 约束：置顶上限 {@link ChatConstants#MAX_PINNED_CONVERSATIONS} 个，超限时 Service 抛业务异常。
     */
    @PutMapping("/conversations/{id}/pin")
    public Result<Void> pinConversation(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        chatService.setConversationPinned(id, userId, true);
        return Result.success("已置顶", null);
    }

    /**
     * L3-M0-7：会话取消置顶（对称 {@link #pinConversation}）。
     */
    @PutMapping("/conversations/{id}/unpin")
    public Result<Void> unpinConversation(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        chatService.setConversationPinned(id, userId, false);
        return Result.success("已取消置顶", null);
    }

    /**
     * L3-M0-7：会话免打扰（对齐 WhatsApp Mute / 微信消息免打扰）。
     * <p>
     * 语义：消息正常收取 + 未读数正常显示，但不触发前端桌面通知 / 声音提醒。
     */
    @PutMapping("/conversations/{id}/mute")
    public Result<Void> muteConversation(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        chatService.setConversationMuted(id, userId, true);
        return Result.success("已设为免打扰", null);
    }

    /**
     * L3-M0-7：会话取消免打扰（对称 {@link #muteConversation}）。
     */
    @PutMapping("/conversations/{id}/unmute")
    public Result<Void> unmuteConversation(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        chatService.setConversationMuted(id, userId, false);
        return Result.success("已取消免打扰", null);
    }

    /**
     * L3-M1-4：取消归档，把会话从"已归档"视图恢复到主列表（对称 {@link #hideConversation}）。
     * <p>
     * HTTP 语义：PUT 幂等 —— 多次调用结果相同（已是"未归档"时再次调用也不报错）。
     * 权限：必须是会话参与者（Service 层校验）。
     */
    @PutMapping("/conversations/{id}/unhide")
    public Result<Void> unhideConversation(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        chatService.unhideConversation(id, userId);
        return Result.success("已恢复到列表", null);
    }

    /**
     * 查询指定用户在线状态。
     * <p>
     * 安全加固（M4 修复 REST 侧）：仅当调用者在目标用户的可见观察者列表内才返回真实状态，
     * 否则统一返回 false，避免通过枚举 userId 进行用户发现/行为追踪。
     */
    @GetMapping("/online-status/{userId}")
    public Result<Map<String, Object>> getOnlineStatus(@PathVariable Long userId) {
        Long callerId = SecurityUtils.getCurrentUserId();
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("userId", userId);
        boolean canSee = callerId != null
                && chatVisibilityService.getStatusWatchers(userId).contains(callerId);
        result.put("online", canSee && wsEventListener.isUserOnline(userId));
        return Result.success(result);
    }

    /**
     * 批量查询在线状态。
     * <p>
     * 安全加固（M4 修复 REST 侧）：按可见性过滤，未授权的 userId 统一返回 false。
     * 同时防止通过超大 userIds 列表做慢速枚举攻击（限制单次最多 200 个）。
     */
    @PostMapping("/online-status/batch")
    public Result<Map<String, Boolean>> batchGetOnlineStatus(@RequestBody List<Long> userIds) {
        Map<String, Boolean> result = new java.util.LinkedHashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return Result.success(result);
        }
        if (userIds.size() > 200) {
            return Result.error(400, "批量查询数量超限（最多 200）");
        }
        Long callerId = SecurityUtils.getCurrentUserId();
        if (callerId == null) {
            for (Long uid : userIds) result.put(String.valueOf(uid), false);
            return Result.success(result);
        }
        for (Long uid : userIds) {
            if (uid == null) continue;
            boolean canSee = chatVisibilityService.getStatusWatchers(uid).contains(callerId);
            result.put(String.valueOf(uid), canSee && wsEventListener.isUserOnline(uid));
        }
        return Result.success(result);
    }
}
