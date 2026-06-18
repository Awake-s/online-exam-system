package com.exam.common.constants;

/**
 * 聊天模块常量
 * 集中管理长度限制、限流配额、幂等窗口等魔法数字
 */
public final class ChatConstants {
    private ChatConstants() {}

    // ========== Redis Key 应用命名空间（阿里开发规约 §1.1 强制） ==========
    /**
     * 全局 Key 前缀 —— 用于防止多项目部署时 Redis Key 冲突（本机 Redis 曾被若依/crmeb 项目污染）。
     * <p>
     * 规范：{@code {应用}:{模块}:{业务}:{id}}，参见
     * <a href="https://developer.aliyun.com/article/557508">阿里云 Redis 开发规范</a>。
     */
    public static final String APP_KEY_NAMESPACE = "exam:";

    /** 单条消息最大字符数（按 Unicode code point 计算，不是 UTF-16 code unit） */
    public static final int MAX_MESSAGE_LENGTH = 4000;

    /** 会话 lastMessage 截取最大字符数（按 code point） */
    public static final int LAST_MESSAGE_PREVIEW_LENGTH = 100;

    // ========== 速率限制（按角色分级） ==========
    /** 管理员：10 msg/秒（批量通知场景） */
    public static final double RATE_LIMIT_ADMIN_PER_SECOND = 10.0;
    /** 教师：2 msg/秒（答疑场景） */
    public static final double RATE_LIMIT_TEACHER_PER_SECOND = 2.0;
    /** 学生：2 msg/秒（防刷屏，同时保证批量发文件/图片/文字不被误伤） */
    public static final double RATE_LIMIT_STUDENT_PER_SECOND = 2.0;
    /** 兜底配额（无法识别角色时使用） */
    public static final double RATE_LIMIT_DEFAULT_PER_SECOND = 2.0;

    // ========== 幂等键 ==========
    /** 幂等键 Redis TTL（秒），足够覆盖客户端 10 秒超时 + 若干次重试 */
    public static final int IDEMPOTENCY_WINDOW_SECONDS = 30;
    /** 幂等键 Redis 前缀 */
    public static final String IDEMPOTENCY_KEY_PREFIX = "chat:idem:";
    /** clientMsgId 最大长度（防止攻击者写超长 key 撑爆 Redis） */
    public static final int CLIENT_MSG_ID_MAX_LENGTH = 64;

    // ========== HTTP 响应 ==========
    /**
     * 超限时返回的 HTTP 状态码（RFC 6585）。
     * <p>
     * 当前项目采用 dropwizard-style（HTTP 200 + body.code 区分业务成败），
     * 限流触发时 Service 层抛 BusinessException，由 GlobalExceptionHandler 转为 body.code=500。
     * 此常量保留作前瞻性契约，未来如需向 REST 响应加 HTTP 429 + Retry-After 头可直接引用。
     */
    public static final int HTTP_TOO_MANY_REQUESTS = 429;
    /** 建议客户端重试等待秒数（在 ChatRateLimiterService 的限流告警日志中引用 + Controller 批量接口 429 Retry-After 头） */
    public static final int RATE_LIMIT_RETRY_AFTER_SECONDS = 5;

    // ========== L3-M3-1：原子批量发送 ==========
    /**
     * 单批最多消息数（对齐 Slack chat.postMessage 建议上限 20 / Discord 单消息最多 10 附件 + 少量文本）。
     * <p>
     * 设计考量：
     * <ul>
     *   <li>20 条已能覆盖"一次贴 10 文件 + 10 图片 + 1 段文字"的极端场景</li>
     *   <li>批次过大会导致单次请求体膨胀 + DB 插入阻塞主线程，20 条平衡性能与用户体验</li>
     *   <li>超过时前端应拆分多批，批间自行节流（仍受 2 msg/s 的批次级限流约束）</li>
     * </ul>
     */
    public static final int MAX_BATCH_SEND_SIZE = 20;

    // ========== L3：消息撤回 / 删除 ==========
    /**
     * 发送者撤回时限（秒）。
     * <p>
     * 业界参考：WhatsApp ~68 分钟、Telegram 48 小时、Signal 3 小时、Slack 无时限。
     * 本系统定位教学场景，偏保守：2 分钟内允许撤回，覆盖"误发/发错人"常见场景；
     * 超出时限需联系管理员强删（含审计）。
     */
    public static final long MESSAGE_RECALL_WINDOW_SECONDS = 120;
    /** 撤回后占位文案（前端可按 L3 规范自行渲染为"此消息已撤回"） */
    public static final String DELETED_MESSAGE_PLACEHOLDER = "[此消息已撤回]";

    // ========== L3-M0-4：撤回后"重新编辑"草稿 ==========
    /**
     * 撤回后"重新编辑"草稿保留秒数。
     * <p>
     * 业界参考：微信 / QQ 均为 2 分钟。本系统与 MESSAGE_RECALL_WINDOW_SECONDS 同值，
     * 语义是"撤回后能编辑多久"。过期后前端按钮消失 + 后端接口返回 410 Gone。
     */
    public static final long RECALL_DRAFT_TTL_SECONDS = 120;
    /** 撤回草稿 Redis 前缀（key = {prefix}{messageId}） */
    public static final String RECALL_DRAFT_KEY_PREFIX = "chat:recall:draft:";

    // ========== L3-M0-6：Typing Indicator（"对方正在输入..."）==========
    /**
     * 每对 sender→receiver 的 typing 事件速率上限（次/秒）。
     * <p>
     * 业界参考：WhatsApp 在一次事件驱动通信中把 typing 作为"disposable event"，
     * 单次打字会话约每 3~5 秒产生一次 START 心跳 + 1 次 STOP；设置 2 次/秒
     * 足以吸收客户端 debounce 误差，同时阻断恶意刷屏。
     */
    public static final int TYPING_RATE_LIMIT_PER_SECOND = 2;
    /** Typing 速率限制 Redis key 前缀（key = {prefix}{senderId}:{receiverId}） */
    public static final String TYPING_RATE_KEY_PREFIX = "chat:typing:rate:";
    /**
     * 前端 typing 心跳间隔（毫秒）— 用户持续输入时每 3 秒重发一次 TYPING_START。
     * 对齐 WhatsApp 文档中的 3s 心跳频率。此常量仅语义参考，前端实现使用。
     */
    public static final int TYPING_HEARTBEAT_MS = 3000;
    /**
     * 前端 typing 自动 STOP 延迟（毫秒）— 最后一次键入后 5 秒自动发 TYPING_STOP。
     */
    public static final int TYPING_AUTO_STOP_MS = 5000;
    /**
     * 接收端 typing 提示自动过期（毫秒）— 兜底防止因 STOP 事件丢包导致 UI 永远悬停。
     * 必须 > TYPING_HEARTBEAT_MS，否则持续打字状态会闪烁；取 6s = 2 倍心跳间隔。
     */
    public static final int TYPING_CLIENT_TTL_MS = 6000;

    // ========== L3-bugfix TYPING-001：聊天权限单一事实源缓存 ==========
    /**
     * {@link com.exam.service.ChatPermissionService#canChatSilent} 的 Redis 缓存 key 前缀。
     * <p>
     * 方向敏感，完整 key 形态：{@code chat:perm:s{senderId}:r{receiverId}}。
     * 值为字符串 {@code "1"}（允许）或 {@code "0"}（拒绝）。
     */
    public static final String PERM_CACHE_KEY_PREFIX = "chat:perm:";
    /**
     * 聊天权限缓存 TTL（秒）—— 平衡"角色变更即时性"与"typing 高频打库"的关键参数。
     * <p>
     * 取值 30s 的工程依据：
     * <ul>
     *   <li>角色变更（如学生转班）属稀有事件，30s 延迟对用户基本无感</li>
     *   <li>typing 心跳 3s/次，30s 内同一对 sender-receiver 最多穿透 1 次 DB 查询</li>
     *   <li>对标 Microsoft Teams Education organizational-graph 缓存的同量级配置</li>
     * </ul>
     */
    public static final long PERM_CACHE_TTL_SECONDS = 30L;

    // ========== L3-M0-7：会话置顶 + 免打扰 ==========
    /**
     * 单用户置顶会话上限。
     * <p>
     * 业界参考：
     * <ul>
     *   <li>Telegram — 5 个</li>
     *   <li>WhatsApp — 3 个</li>
     *   <li>微信 — 无限（但实际体验上超过 10 个会非常拥挤）</li>
     * </ul>
     * 本系统教学场景下取 5（可容纳班主任/学委/教务员等多关键联系人），
     * 超过时 Service 层抛业务异常，由前端提示"最多可置顶 5 个会话"。
     */
    public static final int MAX_PINNED_CONVERSATIONS = 5;

    // ========== 认证模块 Redis Key（集中管理，阿里开发规约 §6.1） ==========
    /**
     * JWT 黑名单 Key 前缀 —— 用户登出时登记已失效的 Token。
     * <p>
     * 完整 Key：{@code blacklist:token:{完整JWT字符串}}；
     * TTL = JWT 剩余寿命（最长 24h，由 {@code jwt.expiration} 决定）；
     * 值：{@code "1"}（占位，仅用作存在性标记）。
     * <p>
     * 保持历史前缀 {@code blacklist:token:} 向后兼容（避免升级瞬间黑名单失效导致已登出
     * Token 重新可用的安全窗口）。未来若全面启用 {@link #APP_KEY_NAMESPACE} 迁移，
     * 需配合一次性 MIGRATE 脚本。
     */
    public static final String AUTH_BLACKLIST_KEY_PREFIX = "blacklist:token:";

    // ========== 通知去重 Redis Key（P0-3 重构，消除 TOCTOU 竞态） ==========
    /**
     * 通知去重 Key 前缀。
     * <p>
     * 完整 Key：{@code notification:dedup:{SHA-256(userId:type:bizId) 前 16 位}}；
     * 值：{@code "1"}（占位）；
     * TTL：{@link #NOTIFY_DEDUP_WINDOW_SECONDS}。
     * <p>
     * 作用：阅卷流程可能多次触发同一业务事件（如"成绩发布"），用本 Key 防止在 5 分钟
     * 窗口内给同一用户推送重复通知，减少打扰。
     */
    public static final String NOTIFY_DEDUP_KEY_PREFIX = "notification:dedup:";

    /**
     * 通知去重窗口（秒）—— 5 分钟。
     * <p>
     * 取值依据：阅卷→评分→发布的完整流程通常在 2 分钟内完成，5 分钟窗口足以覆盖
     * 所有可能的重放路径。过短会导致"正常多次通知"被误拦，过长会延后合法重试。
     */
    public static final int NOTIFY_DEDUP_WINDOW_SECONDS = 300;
}
