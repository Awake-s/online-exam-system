package com.exam.service;

import com.exam.common.exception.BusinessException;

/**
 * L3-bugfix TYPING-001：聊天关系权限 —— 单一事实源（Single Source of Truth）。
 * <p>
 * <b>背景</b>：旧版 {@code ChatTypingController} 用「{@code chat_conversation} 表存在性」
 * 作为权限校验代理，借用"会话存在 ⇒ 双方权限已通过 sendMessage 路径校验"的隐式推论。
 * 该设计在"零会话首次 typing"场景彻底失效 —— 用户新打开一个从未发过消息的联系人，
 * typing 事件被后端静默丢弃，对方界面看不到"正在输入..."；直到首条消息送达后
 * {@link com.exam.entity.ChatConversation} 行被 upsert，typing 才恢复正常。
 * 该 bug 与 CHAT-325（peer-keyed 草稿）根源相同：<b>误把派生标识（conversationId）
 * 当稳定标识</b>。
 *
 * <h3>业界对标（真实教育业务场景的最贴近参考）</h3>
 * 你的系统是 role-based（学生/教师/管理员） + class-scoped（班级关系）的封闭组织架构。
 * 与之最贴合的业界产品是 <b>Microsoft Teams Education</b> 与 <b>Google Chat Workspace</b>，
 * 它们的 typing indicator 实现原则完全一致：
 * <ul>
 *   <li><b>权限服务单一事实源</b>：typing 和 sendMessage 走同一个 permission gate，
 *       避免两处实现漂移导致"能发消息但看不到 typing"或反之的不一致状态</li>
 *   <li><b>短 TTL 缓存</b>：typing 事件频率高（~2 次/秒），权限结果缓存 30s
 *       避免打穿数据库；角色变更等稀有事件的缓存陈旧可接受</li>
 *   <li><b>权限失败静默丢弃</b>：typing 是 UX 锦上添花，不应暴露具体错误原因给前端，
 *       对攻击者也更难用作权限探测工具</li>
 *   <li><b>无 conversation 副作用</b>：不在 typing 路径 upsert 会话表，避免"用户只打字不
 *       发送"场景产生大量空会话行</li>
 * </ul>
 *
 * <h3>与 XMPP XEP-0085 的关系</h3>
 * XMPP §4.1 Rule 3 要求首条消息前禁止 typing notification，但该规则针对<b>联邦化异构客户端的
 * 能力协商</b>（不知道对端客户端支不支持 chat state）。集中式单厂商系统（你这种）双端能力绝对
 * 同构，不适用该约束。Telegram/WhatsApp/Slack/Teams 都不遵守此规则。
 *
 * <h3>安全与容错</h3>
 * <ol>
 *   <li><b>Fail-safe</b>：{@link #canChatSilent} 对任何异常均返回 {@code false}（拒绝）。
 *       Redis 故障、DB 故障、用户不存在等任一错误都不会误放行</li>
 *   <li><b>方向敏感</b>：权限矩阵不对称（教师→学生 vs 学生→教师规则不同），故缓存 key
 *       包含方向信息 {@code chat:perm:s{senderId}:r{receiverId}}</li>
 *   <li><b>不缓存异常</b>：若 {@link #assertCanChat} 调用链抛出非 {@link BusinessException}
 *       （如 DB 连接失败），{@link #canChatSilent} 返回 false 但<b>不写缓存</b>，
 *       下次可重试</li>
 * </ol>
 *
 * @author Cascade
 * @since L3-bugfix TYPING-001（CHAT-325 零会话 typing 根治关联）
 */
public interface ChatPermissionService {

    /**
     * 校验 {@code senderId} 是否有权限与 {@code receiverId} 建立聊天会话，
     * 不通过时抛出 {@link BusinessException} 附带<b>具体拒绝原因</b>。
     * <p>
     * 调用方：{@code sendMessage} / {@code sendMessagesBatch} —— 需要把具体错误文案
     * 透传给前端（如"学生之间不允许聊天"）。
     * <p>
     * <b>不使用缓存</b>：消息发送路径低频（≤ 2 次/秒限流），总是实时校验最新状态，
     * 避免权限变更后 30s 内仍能发消息的安全风险。
     *
     * @param senderId   发送者用户 ID
     * @param receiverId 接收者用户 ID
     * @throws BusinessException 任一校验失败时抛出，message 为用户可读的中文原因
     */
    void assertCanChat(Long senderId, Long receiverId);

    /**
     * 静默版本：返回 {@code true/false} + Redis 30s 缓存，用于 typing indicator 等
     * 高频且失败可静默的场景。
     * <p>
     * 调用方：{@link com.exam.controller.ChatTypingController#handleTyping}
     * <p>
     * <b>缓存策略</b>：key = {@code chat:perm:s{sender}:r{receiver}}，TTL = 30s。
     * 底层命中 miss 时调用 {@link #assertCanChat}；BusinessException 捕获后
     * 缓存 {@code "0"}；其它异常（如 DB 故障）返回 false 但<b>不写缓存</b>，下次重试。
     *
     * @param senderId   发送者用户 ID
     * @param receiverId 接收者用户 ID
     * @return {@code true} 有权限聊天，{@code false} 无权限或任一异常
     */
    boolean canChatSilent(Long senderId, Long receiverId);
}
