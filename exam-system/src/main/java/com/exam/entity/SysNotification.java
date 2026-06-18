package com.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 通知实体类（E1 业界顶级架构升级版）。
 * <p>
 * <b>E1 新增字段</b>：
 * <ul>
 *   <li>{@code priority} - 优先级（1=紧急 / 2=普通 / 3=次要），UI 色标差异化渲染</li>
 *   <li>{@code payload}  - 扩展载荷 JSON，承载 senderId/senderName/senderAvatar/actionUrl/extras 等</li>
 * </ul>
 * <p>
 * <b>为什么用 payload JSON 而不是单独列</b>：
 * 对齐 Linear/Slack/GitHub 的通知 schema 设计，一次建表永久可扩展，
 * 所有未来需求（富内容、富链接、富元数据）都走 payload，避免频繁 ALTER TABLE。
 */
@Data
@TableName("sys_notification")
public class SysNotification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String type;
    private String title;
    private String content;
    private String bizType;
    private Long bizId;
    private Integer isRead;

    /** 优先级：1=紧急 / 2=普通 / 3=次要（默认 2） */
    private Integer priority;

    /**
     * 扩展载荷 JSON 字符串。
     * <p>典型结构：{@code {"senderId":1,"senderName":"张三","senderAvatar":"...","actionUrl":"/path","extras":{}}}
     * <p>MyBatis-Plus 默认以 String 映射 JSON 列，Service 层转换 Map。
     */
    private String payload;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
