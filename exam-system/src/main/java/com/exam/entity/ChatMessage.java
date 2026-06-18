package com.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("chat_message")
public class ChatMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long conversationId;
    private Long senderId;
    private Long receiverId;
    private String content;
    private Integer messageType;
    private Integer isRead;
    private LocalDateTime createTime;
    /**
     * L3：软删时间。NULL 表示未删除；非 NULL 表示该消息已被撤回或管理员强删，
     * 查询端应过滤 deleted_at IS NULL 或渲染为"已撤回"占位。
     */
    private LocalDateTime deletedAt;
    /**
     * L3：删除人用户 ID（发送者撤回=自身；管理员强删=管理员 ID）。
     * 与 deletedAt 配套使用，用于审计追踪。
     */
    private Long deletedBy;
}
