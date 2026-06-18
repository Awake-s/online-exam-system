package com.exam.service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 通知发送选项（E1 架构升级 - Builder 模式）。
 * <p>
 * <b>设计目的</b>：把通知的"扩展维度"（优先级、发送者、action_url、自定义 extras）
 * 聚合成单一对象，避免 Service 接口签名因扩展需求而无限膨胀。
 * <p>
 * <b>向后兼容</b>：所有字段均为可选（null 表示使用默认值）。
 * 旧 {@code notifyUser(...)} 签名保留，内部默认传 {@link #defaults()}，行为零变化。
 * <p>
 * <b>典型用法</b>：
 * <pre>{@code
 * // 紧急通知 + 发送者
 * NotificationOptions.priority(1)
 *         .sender(teacher.getId(), teacher.getRealName(), teacher.getAvatar())
 *         .actionUrl("/student/exam?id=" + examId);
 *
 * // 普通通知 + 自定义 extras
 * NotificationOptions.defaults()
 *         .extra("score", 95)
 *         .extra("passLine", 60);
 * }</pre>
 * <p>
 * <b>参考设计</b>：Slack {@code chat.postMessage} attachments、
 * GitHub Notifications {@code subject} 字段、Linear {@code notification.metadata}。
 */
public class NotificationOptions {

    /** 优先级：1=紧急 / 2=普通（默认）/ 3=次要 */
    public static final int PRIORITY_URGENT = 1;
    public static final int PRIORITY_NORMAL = 2;
    public static final int PRIORITY_LOW = 3;

    /** 默认优先级 */
    private static final int DEFAULT_PRIORITY = PRIORITY_NORMAL;

    private Integer priority = DEFAULT_PRIORITY;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private String actionUrl;
    private Map<String, Object> extras;

    /** 静态工厂：默认配置（priority=2 普通，无 sender，无 actionUrl） */
    public static NotificationOptions defaults() {
        return new NotificationOptions();
    }

    /** 静态工厂：指定优先级的快捷构造 */
    public static NotificationOptions priority(Integer priority) {
        return new NotificationOptions().withPriority(priority);
    }

    /** 静态工厂：紧急通知（priority=1） */
    public static NotificationOptions urgent() {
        return priority(PRIORITY_URGENT);
    }

    /** 链式 setter - 优先级 */
    public NotificationOptions withPriority(Integer priority) {
        if (priority != null && priority >= 1 && priority <= 3) {
            this.priority = priority;
        }
        return this;
    }

    /** 链式 setter - 发送者信息（三项一起设置保证一致性） */
    public NotificationOptions withSender(Long senderId, String senderName, String senderAvatar) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderAvatar = senderAvatar;
        return this;
    }

    /**
     * 链式 setter - 目标路由（前端 router.push 直接使用）。
     * <p>
     * <b>安全约束（E1 - OWASP A10 防开放重定向 + A1 防存储型 XSS）</b>：
     * 仅接受站内相对路径（必须以 `/` 开头），不得以 `//` 开头（protocol-relative），
     * 不得包含 {@code javascript:/data:/vbscript:/file:} 等 scheme。
     * 不合法的 URL 将被静默丢弃（保持 {@code null}），由前端回退到硬编码逻辑兜底。
     * <p>
     * 当前所有调用点均由开发者控制生成（无用户输入污染），此校验属于纵深防御，
     * 避免未来任何调用点误引入用户输入造成持久化攻击入口。
     */
    public NotificationOptions withActionUrl(String actionUrl) {
        if (isSafeActionUrl(actionUrl)) {
            this.actionUrl = actionUrl;
        }
        return this;
    }

    /**
     * URL 白名单校验（与前端 {@code useNotificationHandler.ts#isSafeActionUrl} 保持一致）。
     * 返回 {@code true} 表示 URL 安全可落库。
     */
    private static boolean isSafeActionUrl(String url) {
        if (url == null) return false;
        int len = url.length();
        if (len == 0 || len > 500) return false;
        // 必须以 / 开头
        if (url.charAt(0) != '/') return false;
        // 禁止 // 开头的 protocol-relative URL
        if (len > 1 && url.charAt(1) == '/') return false;
        // 禁止 scheme 前缀（大小写不敏感，容忍前导空白）
        String trimmed = url.trim().toLowerCase();
        return !(trimmed.startsWith("javascript:")
                || trimmed.startsWith("data:")
                || trimmed.startsWith("vbscript:")
                || trimmed.startsWith("file:"));
    }

    /** 链式 setter - 添加单个 extras 键值对 */
    public NotificationOptions extra(String key, Object value) {
        if (extras == null) extras = new LinkedHashMap<>();
        extras.put(key, value);
        return this;
    }

    /** 链式 setter - 一次性设置 extras Map */
    public NotificationOptions withExtras(Map<String, Object> extras) {
        if (extras != null && !extras.isEmpty()) {
            if (this.extras == null) this.extras = new LinkedHashMap<>();
            this.extras.putAll(extras);
        }
        return this;
    }

    // ===================== Getters =====================
    public Integer getPriority() { return priority != null ? priority : DEFAULT_PRIORITY; }
    public Long getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public String getSenderAvatar() { return senderAvatar; }
    public String getActionUrl() { return actionUrl; }
    public Map<String, Object> getExtras() { return extras; }

    /** 判断是否有任何发送者信息 */
    public boolean hasSender() {
        return senderId != null || senderName != null || senderAvatar != null;
    }

    /** 判断是否需要生成 payload（任一字段非默认即生成） */
    public boolean hasPayloadData() {
        return hasSender() || actionUrl != null || (extras != null && !extras.isEmpty());
    }

    /**
     * 构造 payload Map（供序列化为 JSON）。
     * 空字段不纳入 Map，保持 payload 紧凑。
     */
    public Map<String, Object> buildPayloadMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        if (senderId != null) map.put("senderId", senderId);
        if (senderName != null) map.put("senderName", senderName);
        if (senderAvatar != null) map.put("senderAvatar", senderAvatar);
        if (actionUrl != null) map.put("actionUrl", actionUrl);
        if (extras != null && !extras.isEmpty()) map.put("extras", extras);
        return map;
    }
}
