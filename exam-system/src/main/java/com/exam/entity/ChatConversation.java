package com.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("chat_conversation")
public class ChatConversation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long user1Id;
    private Long user2Id;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    /**
     * L3-M0-5：最后一条消息的发送者 ID。
     * <p>
     * 对齐微信/QQ/WhatsApp 会话表标准：前端据此判断"你撤回了一条消息" vs "对方撤回了一条消息"。
     * 未来扩展群聊时，可用于渲染"张三撤回了一条消息"（查 user.realName）。
     * 历史会话可为 NULL；新增消息时 {@code sendMessage} / 撤回时 {@code refreshConversationLastMessage}
     * 同步维护此字段。
     */
    private Long lastMessageSenderId;
    private LocalDateTime createTime;
    /**
     * L3：user1 是否已从会话列表隐藏该会话。
     * 0=显示（默认），1=已隐藏；当收到对方新消息时重置为 0 以重新可见。
     */
    private Integer user1Hidden;
    /**
     * L3：user2 是否已从会话列表隐藏该会话。同上。
     */
    private Integer user2Hidden;

    /**
     * L3-M0-7：user1 是否将该会话置顶。
     * <p>
     * 业界参考：微信/WhatsApp/Telegram 的 Pin Conversation。
     * 置顶会话永远排在普通会话之上，上限 5 个（与 Telegram 一致）。
     * 0=未置顶（默认），1=已置顶。
     */
    private Integer user1Pinned;
    /** L3-M0-7：user2 置顶标志，同 user1Pinned。 */
    private Integer user2Pinned;

    /**
     * L3-M0-7：user1 是否对该会话免打扰（Mute）。
     * <p>
     * 业界参考：WhatsApp Mute / 微信消息免打扰。语义：
     * <ul>
     *   <li>消息正常收取（进入 DB + WS 推送）</li>
     *   <li>未读数正常显示</li>
     *   <li>但不触发桌面通知 / 系统提示音</li>
     * </ul>
     * 与 hidden（归档）互不干扰：可以既归档又免打扰。
     * 0=未免打扰（默认），1=已免打扰。
     */
    private Integer user1Muted;
    /** L3-M0-7：user2 免打扰标志，同 user1Muted。 */
    private Integer user2Muted;
}
