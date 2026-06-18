package com.exam.service;

import java.util.List;
import java.util.Map;

import com.exam.common.result.PageResult;

public interface ChatService {
    List<Map<String, Object>> getContacts(Long userId, Long roleId, Long classId);
    List<Map<String, Object>> getContacts(Long userId);
    List<Map<String, Object>> getConversations(Long userId);

    /**
     * L3-M1-4：按归档状态过滤会话列表。
     * <p>
     * 采用 Telegram/WhatsApp 的 Archive 模式：归档（hidden=1）是"从主列表移除 + 独立可访问"，
     * 不等价于删除。此方法让前端能在"主列表"和"归档列表"两个视图间切换。
     *
     * @param userId   当前用户
     * @param archived true=只返回被当前用户归档的会话；false=只返回未归档会话（等同原 getConversations）
     */
    List<Map<String, Object>> getConversations(Long userId, boolean archived);
    PageResult<Map<String, Object>> getMessages(Long conversationId, Long userId, Integer page, Integer size);
    Map<String, Object> sendMessage(Long senderId, Long receiverId, String content);
    /**
     * 发送消息（带客户端幂等键版本）
     * @param clientMsgId 客户端幂等键，null 时降级为非幂等处理
     */
    Map<String, Object> sendMessage(Long senderId, Long receiverId, String content, String clientMsgId);

    /**
     * L3-M3-1：原子批量发送消息（对齐 Slack chat.postMessage batch / Telegram sendMediaGroup / Discord 单消息多附件）。
     * <p>
     * <b>核心设计：</b>
     * <ul>
     *   <li><b>批次级限流</b>：整批只消耗 1 个 RateLimiter permit（而非 N 个），彻底解决"批量发 5 文件第 3 条起被限流"的业务痛点</li>
     *   <li><b>Partial Success</b>：每条消息独立 try/catch，单条失败不影响其他条；返回 per-item 结果让前端精确定位失败项</li>
     *   <li><b>共享校验</b>：发送者身份、对方账号状态、角色间通信权限、会话查找/创建、考试中检测 —— 全批次只执行 1 次，性能翻倍</li>
     *   <li><b>独立幂等</b>：每项 clientMsgId 独立抢占 Redis slot；重复提交自动命中已有消息返回 {@code already_sent}</li>
     *   <li><b>严格有序</b>：批次内按 items 顺序串行插入 DB，保证接收方消息时间戳与顺序一致</li>
     * </ul>
     * <p>
     * <b>返回格式</b>：{@code List<Map>}，每项含：
     * <ul>
     *   <li>{@code clientMsgId}：回传客户端幂等键</li>
     *   <li>{@code status}：{@code sent}（新发成功）/ {@code already_sent}（幂等命中）/ {@code failed}（单条失败，见 error 字段）</li>
     *   <li>{@code message}：{@code status=sent|already_sent} 时包含完整消息对象（id/content/createTime 等）</li>
     *   <li>{@code error}：{@code status=failed} 时的错误说明</li>
     * </ul>
     * <p>
     * <b>限流触发</b>：批次级 tryAcquire 失败时直接抛 {@code BusinessException("发送过于频繁...")}，由 Controller 转 HTTP 429 + Retry-After。
     *
     * @param senderId   发送者 ID
     * @param receiverId 统一接收者（当前版本不支持跨会话批量，简化幂等与推送语义）
     * @param items      每项含 {@code clientMsgId}（必填）与 {@code content}（必填）
     * @return 每条的处理结果，顺序与 items 一致
     * @throws com.exam.common.exception.BusinessException 批次级限流 / sender/receiver 不存在 / 角色间通信违规
     */
    List<Map<String, Object>> sendMessagesBatch(Long senderId, Long receiverId, List<Map<String, String>> items);

    void markAsRead(Long conversationId, Long userId);
    Map<String, Object> getUnreadCount(Long userId);

    /**
     * I2：WS 重连后补拉漏消息。
     * <p>
     * 返回当前用户作为 sender 或 receiver、且 message.id 大于 sinceMessageId 的所有消息，
     * 按 id 升序排列，单次最多返回 limit 条（服务端最高限 500，防止放大攻击）。
     *
     * @param userId         当前用户 ID
     * @param sinceMessageId 已获取到的最大消息 ID（首次调用可传 0 或 null）
     * @param limit          单次上限，服务端自动约束到 [1, 500]
     * @return 增量消息列表（已按 id 升序），空列表表示无漏消息
     */
    List<Map<String, Object>> getIncrementalMessages(Long userId, Long sinceMessageId, Integer limit);

    // ========== L3：消息撤回 / 删除 / 会话隐藏 ==========

    /**
     * L3：发送者撤回自己发出的消息（有时限，详见 ChatConstants.MESSAGE_RECALL_WINDOW_SECONDS）。
     * <p>
     * 校验：必须是 messageId 的 sender 本人；消息未被删除；创建时间在撤回窗口内。
     * 执行成功后通过 WS 推送 {@code {type:'MESSAGE_DELETED', messageId, conversationId}} 给会话双方。
     *
     * @param messageId 要撤回的消息 ID
     * @param userId    当前用户 ID
     * @throws com.exam.common.exception.BusinessException 超时/非本人/消息不存在/已删
     */
    void recallMessage(Long messageId, Long userId);

    /**
     * L3-M0-4：获取撤回消息"重新编辑"草稿（对齐微信/QQ 2 分钟内可编辑）。
     * <p>
     * 校验：
     * <ul>
     *   <li>消息必须存在且已被撤回（deleted_at 非空）</li>
     *   <li>调用者必须是原发送者（{@code userId == senderId}）</li>
     *   <li>消息类型必须是文字（messageType=1），图片/文件无法重新编辑</li>
     *   <li>距撤回时间不超过 {@link com.exam.common.constants.ChatConstants#RECALL_DRAFT_TTL_SECONDS}</li>
     * </ul>
     *
     * @return 消息原文，或 null 表示草稿已过期/不存在
     * @throws com.exam.common.exception.BusinessException 非发送者 / 消息不存在 / 消息未撤回
     */
    String getRecallDraft(Long messageId, Long userId);

    /**
     * L3：管理员强制删除任意消息（不受时限限制）。
     * <p>
     * 校验：调用者必须是管理员。会写入审计日志便于追踪。
     *
     * @param messageId 目标消息 ID
     * @param adminId   管理员用户 ID
     */
    void adminDeleteMessage(Long messageId, Long adminId);

    /**
     * L3：当前用户从自己的会话列表中隐藏该会话（不影响对方，不删除消息）。
     * <p>
     * 当对方或自己发新消息时，隐藏状态会被自动清除（重新显示）。
     *
     * @param conversationId 会话 ID
     * @param userId         当前用户 ID
     */
    void hideConversation(Long conversationId, Long userId);

    /**
     * L3-M0-7：会话置顶 / 取消置顶（幂等）。
     * <p>
     * 业界参考：微信 / WhatsApp / Telegram 的 Pin Conversation。
     * 置顶会话在列表中永远排在普通会话之上（Service 层排序 + 前端也据此渲染）。
     * <p>
     * 约束：
     * <ul>
     *   <li>置顶上限 {@link com.exam.common.constants.ChatConstants#MAX_PINNED_CONVERSATIONS} 个，
     *       超过时抛 {@code BusinessException}</li>
     *   <li>幂等：已置顶时再调 pinned=true 无害（不抛错）</li>
     *   <li>仅当前用户视角，对方不受影响（user1_pinned / user2_pinned 两列独立）</li>
     * </ul>
     *
     * @param conversationId 会话 ID
     * @param userId         当前用户 ID
     * @param pinned         true=置顶，false=取消置顶
     */
    void setConversationPinned(Long conversationId, Long userId, boolean pinned);

    /**
     * L3-M0-7：会话免打扰 / 取消免打扰（幂等）。
     * <p>
     * 业界参考：WhatsApp Mute / 微信消息免打扰。
     * <p>
     * 语义：
     * <ul>
     *   <li>消息仍会进入 DB + WS 推送</li>
     *   <li>未读数正常显示</li>
     *   <li>但接收端前端不触发桌面通知 / 声音提醒</li>
     * </ul>
     *
     * @param conversationId 会话 ID
     * @param userId         当前用户 ID
     * @param muted          true=静音，false=取消静音
     */
    void setConversationMuted(Long conversationId, Long userId, boolean muted);

    /**
     * L3-M1-4：取消归档，把会话从"已归档"视图恢复到主会话列表（对称 {@link #hideConversation}）。
     * <p>
     * 业务规则：
     * <ul>
     *   <li>只允许会话参与者操作，且只影响调用者自己的视图（hidden 字段 per-user 独立）</li>
     *   <li>执行成功后通过 WS 推送 {@code {type:'CONVERSATION_UNHIDDEN', conversationId}}
     *       给当前用户所有 session，实现多标签页秒级同步</li>
     * </ul>
     *
     * @param conversationId 会话 ID
     * @param userId         当前用户 ID
     */
    void unhideConversation(Long conversationId, Long userId);
}
