package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.common.constants.ChatConstants;
import com.exam.common.constants.RoleConstants;
import com.exam.common.exception.BusinessException;
import com.exam.common.result.PageResult;
import com.exam.entity.*;
import com.exam.mapper.*;
import com.exam.service.ChatIdempotencyService;
import com.exam.service.ChatPermissionService;
import com.exam.service.ChatRateLimiterService;
import com.exam.service.ChatRecallDraftService;
import com.exam.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements ChatService {

    @Autowired private ChatConversationMapper conversationMapper;
    @Autowired private ChatMessageMapper messageMapper;
    @Autowired private SysUserMapper userMapper;
    @Autowired private TeacherClassMapper teacherClassMapper;
    @Autowired private ExamRecordMapper examRecordMapper;
    @Autowired private EduClassMapper classMapper;
    @Autowired private com.exam.config.WebSocketEventListener wsEventListener;
    @Autowired private ChatRateLimiterService rateLimiterService;
    @Autowired private ChatIdempotencyService idempotencyService;
    @Autowired private ChatRecallDraftService recallDraftService;
    @Autowired private SimpMessagingTemplate messagingTemplate;
    // L3-bugfix TYPING-001：聊天权限单一事实源（替代 sendMessage/sendMessagesBatch 内联的重复校验代码）
    @Autowired private ChatPermissionService permissionService;

    @Override
    public List<Map<String, Object>> getContacts(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) return Collections.emptyList();
        return getContacts(userId, user.getRoleId(), user.getClassId());
    }

    @Override
    public List<Map<String, Object>> getContacts(Long userId, Long roleId, Long classId) {
        List<SysUser> contacts = new ArrayList<>();

        if (RoleConstants.ADMIN_ROLE_ID.equals(roleId)) {
            contacts = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                    .ne(SysUser::getId, userId).eq(SysUser::getStatus, 1));
        } else if (RoleConstants.TEACHER_ROLE_ID.equals(roleId)) {
            // 教师可联系：所有管理员 + 所有教师 + 自己班级的学生
            List<SysUser> adminsAndTeachers = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                    .ne(SysUser::getId, userId).eq(SysUser::getStatus, 1)
                    .in(SysUser::getRoleId, RoleConstants.ADMIN_ROLE_ID, RoleConstants.TEACHER_ROLE_ID));
            contacts.addAll(adminsAndTeachers);

            List<TeacherClass> tcList = teacherClassMapper.selectList(
                    new LambdaQueryWrapper<TeacherClass>().eq(TeacherClass::getTeacherId, userId));
            Set<Long> myClassIds = tcList.stream().map(TeacherClass::getClassId).collect(Collectors.toSet());
            if (!myClassIds.isEmpty()) {
                List<SysUser> students = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getRoleId, RoleConstants.STUDENT_ROLE_ID)
                        .eq(SysUser::getStatus, 1).in(SysUser::getClassId, myClassIds));
                contacts.addAll(students);
            }
        } else if (RoleConstants.STUDENT_ROLE_ID.equals(roleId)) {
            // 学生可联系：所有管理员 + 教自己班级的教师（禁止学生互聊）
            List<SysUser> admins = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getRoleId, RoleConstants.ADMIN_ROLE_ID).eq(SysUser::getStatus, 1));
            contacts.addAll(admins);

            if (classId != null) {
                List<TeacherClass> tcList = teacherClassMapper.selectList(
                        new LambdaQueryWrapper<TeacherClass>().eq(TeacherClass::getClassId, classId));
                Set<Long> teacherIds = tcList.stream().map(TeacherClass::getTeacherId).collect(Collectors.toSet());
                if (!teacherIds.isEmpty()) {
                    List<SysUser> teachers = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                            .in(SysUser::getId, teacherIds).eq(SysUser::getStatus, 1));
                    contacts.addAll(teachers);
                }
            }
        }

        // 批量查询所有涉及的班级名称
        Set<Long> allClassIds = contacts.stream()
                .map(SysUser::getClassId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> classNameMap = new HashMap<>();
        if (!allClassIds.isEmpty()) {
            classMapper.selectBatchIds(allClassIds).forEach(c -> classNameMap.put(c.getId(), c.getClassName()));
        }

        return contacts.stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("realName", u.getRealName());
            m.put("avatar", u.getAvatar());
            m.put("roleId", u.getRoleId());
            String roleName = RoleConstants.ADMIN_ROLE_ID.equals(u.getRoleId()) ? "管理员" :
                    RoleConstants.TEACHER_ROLE_ID.equals(u.getRoleId()) ? "教师" : "学生";
            m.put("roleName", roleName);
            m.put("classId", u.getClassId());
            m.put("className", u.getClassId() != null ? classNameMap.getOrDefault(u.getClassId(), "") : "");
            m.put("online", wsEventListener.isUserOnline(u.getId()));
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getConversations(Long userId) {
        // 保持向后兼容：无参版本默认返回"未归档"会话（主列表），与旧行为一致
        return getConversations(userId, false);
    }

    @Override
    public List<Map<String, Object>> getConversations(Long userId, boolean archived) {
        // 使用嵌套 and(..or..) 保证 OR 条件被括号包裹，未来扩展其他过滤条件时不会因 AND/OR 优先级踩坑
        //
        // L3-M1-4：archived 参数控制是查"主列表"还是"归档列表"：
        //   - archived=false (默认)：hidden IS NULL 或 hidden=0，即主列表
        //     生成 SQL：WHERE ((user1_id=? AND (user1_hidden IS NULL OR user1_hidden=0))
        //                    OR (user2_id=? AND (user2_hidden IS NULL OR user2_hidden=0)))
        //   - archived=true：hidden=1，即当前用户已归档的会话
        //     生成 SQL：WHERE ((user1_id=? AND user1_hidden=1)
        //                    OR (user2_id=? AND user2_hidden=1))
        //
        // 为什么要把 IS NULL 保留在未归档分支：兼容 L3 迁移前的老数据（hidden 列为 NULL）。
        // 归档分支不需要 IS NULL 判断，因为 NULL 肯定不等于 1。
        LambdaQueryWrapper<ChatConversation> wrapper = new LambdaQueryWrapper<ChatConversation>()
                .and(w -> {
                    if (archived) {
                        w.and(w1 -> w1.eq(ChatConversation::getUser1Id, userId)
                                      .eq(ChatConversation::getUser1Hidden, 1))
                         .or(w1 -> w1.eq(ChatConversation::getUser2Id, userId)
                                      .eq(ChatConversation::getUser2Hidden, 1));
                    } else {
                        w.and(w1 -> w1.eq(ChatConversation::getUser1Id, userId)
                                      .and(w2 -> w2.isNull(ChatConversation::getUser1Hidden)
                                                   .or().eq(ChatConversation::getUser1Hidden, 0)))
                         .or(w1 -> w1.eq(ChatConversation::getUser2Id, userId)
                                      .and(w2 -> w2.isNull(ChatConversation::getUser2Hidden)
                                                   .or().eq(ChatConversation::getUser2Hidden, 0)));
                    }
                })
                .orderByDesc(ChatConversation::getLastMessageTime);
        List<ChatConversation> convs = conversationMapper.selectList(wrapper);
        if (convs.isEmpty()) return Collections.emptyList();

        // 批量预加载对方用户信息
        Set<Long> otherUserIds = convs.stream()
                .map(c -> c.getUser1Id().equals(userId) ? c.getUser2Id() : c.getUser1Id())
                .collect(Collectors.toSet());
        Map<Long, SysUser> userMap = otherUserIds.isEmpty() ? Collections.emptyMap() :
                userMapper.selectBatchIds(otherUserIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, u -> u));

        // 批量预加载班级名称
        Set<Long> classIds = userMap.values().stream()
                .map(SysUser::getClassId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> classNameMap = classIds.isEmpty() ? Collections.emptyMap() :
                classMapper.selectBatchIds(classIds).stream()
                        .collect(Collectors.toMap(EduClass::getId, EduClass::getClassName));

        // 批量查询未读消息数（SQL 端 GROUP BY，避免将所有未读消息加载到 JVM 内存）
        // 走索引 idx_receiver_read(receiver_id, is_read) + idx_conv_time，无需回表
        Set<Long> convIds = convs.stream().map(ChatConversation::getId).collect(Collectors.toSet());
        Map<Long, Long> unreadCountMap = new HashMap<>();
        if (!convIds.isEmpty()) {
            List<Map<String, Object>> rows = messageMapper.countUnreadByConversations(convIds, userId);
            for (Map<String, Object> row : rows) {
                Object convIdObj = row.get("convId");
                Object cntObj = row.get("cnt");
                if (convIdObj instanceof Number && cntObj instanceof Number) {
                    unreadCountMap.put(((Number) convIdObj).longValue(), ((Number) cntObj).longValue());
                }
            }
        }

        return convs.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            Long otherUserId = c.getUser1Id().equals(userId) ? c.getUser2Id() : c.getUser1Id();
            SysUser other = userMap.get(otherUserId);
            m.put("otherUserId", otherUserId);
            m.put("otherUserName", other != null ? other.getRealName() : "未知用户");
            m.put("otherAvatar", other != null ? other.getAvatar() : null);
            m.put("otherRoleId", other != null ? other.getRoleId() : null);
            m.put("otherClassName", other != null && other.getClassId() != null
                    ? classNameMap.getOrDefault(other.getClassId(), "") : "");
            m.put("lastMessage", c.getLastMessage());
            m.put("lastMessageTime", c.getLastMessageTime());
            // L3-M0-5：最后一条消息的发送者 ID，前端据此渲染"你/对方撤回了一条消息"差异化文案
            m.put("lastMessageSenderId", c.getLastMessageSenderId());
            m.put("unreadCount", unreadCountMap.getOrDefault(c.getId(), 0L));
            // L3-M0-7：按当前用户视角暴露 pinned / muted（对方的同名字段与本用户无关）
            boolean isUser1 = c.getUser1Id().equals(userId);
            Integer pinned = isUser1 ? c.getUser1Pinned() : c.getUser2Pinned();
            Integer muted = isUser1 ? c.getUser1Muted() : c.getUser2Muted();
            m.put("pinned", pinned != null && pinned == 1);
            m.put("muted", muted != null && muted == 1);
            return m;
        })
        // L3-M0-7：置顶会话永远排在普通会话之上（Stream 层排序，避免让 SQL wrapper 复杂化且不走索引）
        // 原 SQL 已按 lastMessageTime DESC 排过序，这里只需稳定地把 pinned 提到前面即可
        .sorted((a, b) -> {
            boolean ap = Boolean.TRUE.equals(a.get("pinned"));
            boolean bp = Boolean.TRUE.equals(b.get("pinned"));
            if (ap != bp) return ap ? -1 : 1;  // 置顶优先
            return 0;  // 置顶组内 / 普通组内保留原有 lastMessageTime DESC 顺序
        })
        .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void setConversationPinned(Long conversationId, Long userId, boolean pinned) {
        ChatConversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) throw new BusinessException("会话不存在");
        boolean isUser1 = conv.getUser1Id().equals(userId);
        boolean isUser2 = conv.getUser2Id().equals(userId);
        if (!isUser1 && !isUser2) throw new BusinessException("无权操作该会话");

        // 幂等：当前值与目标值一致时直接返回，不写 DB 也不报错
        Integer currentVal = isUser1 ? conv.getUser1Pinned() : conv.getUser2Pinned();
        boolean currentlyPinned = currentVal != null && currentVal == 1;
        if (currentlyPinned == pinned) return;

        // L3-M0-7：置顶上限校验（仅对"从未置顶改为置顶"方向有效，取消置顶不受限）
        if (pinned) {
            int targetCol = isUser1 ? 1 : 2;
            Long pinnedCount = conversationMapper.selectCount(new LambdaQueryWrapper<ChatConversation>()
                    .and(w -> {
                        if (targetCol == 1) {
                            w.eq(ChatConversation::getUser1Id, userId)
                             .eq(ChatConversation::getUser1Pinned, 1);
                        } else {
                            w.eq(ChatConversation::getUser2Id, userId)
                             .eq(ChatConversation::getUser2Pinned, 1);
                        }
                    }));
            // 还要加上对称列（user 有可能同时以 user1 或 user2 身份出现在不同会话里）
            int otherCol = targetCol == 1 ? 2 : 1;
            Long pinnedCountOther = conversationMapper.selectCount(new LambdaQueryWrapper<ChatConversation>()
                    .and(w -> {
                        if (otherCol == 1) {
                            w.eq(ChatConversation::getUser1Id, userId)
                             .eq(ChatConversation::getUser1Pinned, 1);
                        } else {
                            w.eq(ChatConversation::getUser2Id, userId)
                             .eq(ChatConversation::getUser2Pinned, 1);
                        }
                    }));
            long total = (pinnedCount == null ? 0 : pinnedCount) + (pinnedCountOther == null ? 0 : pinnedCountOther);
            if (total >= ChatConstants.MAX_PINNED_CONVERSATIONS) {
                throw new BusinessException("最多可置顶 " + ChatConstants.MAX_PINNED_CONVERSATIONS + " 个会话");
            }
        }

        // 仅更新当前用户视角的那一列，对方列不动
        LambdaUpdateWrapper<ChatConversation> update = new LambdaUpdateWrapper<ChatConversation>()
                .eq(ChatConversation::getId, conversationId);
        if (isUser1) update.set(ChatConversation::getUser1Pinned, pinned ? 1 : 0);
        else update.set(ChatConversation::getUser2Pinned, pinned ? 1 : 0);
        conversationMapper.update(null, update);
    }

    @Override
    @Transactional
    public void setConversationMuted(Long conversationId, Long userId, boolean muted) {
        ChatConversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) throw new BusinessException("会话不存在");
        boolean isUser1 = conv.getUser1Id().equals(userId);
        boolean isUser2 = conv.getUser2Id().equals(userId);
        if (!isUser1 && !isUser2) throw new BusinessException("无权操作该会话");

        // 幂等：目标状态与当前一致时直接返回
        Integer currentVal = isUser1 ? conv.getUser1Muted() : conv.getUser2Muted();
        boolean currentlyMuted = currentVal != null && currentVal == 1;
        if (currentlyMuted == muted) return;

        LambdaUpdateWrapper<ChatConversation> update = new LambdaUpdateWrapper<ChatConversation>()
                .eq(ChatConversation::getId, conversationId);
        if (isUser1) update.set(ChatConversation::getUser1Muted, muted ? 1 : 0);
        else update.set(ChatConversation::getUser2Muted, muted ? 1 : 0);
        conversationMapper.update(null, update);
    }

    @Override
    public PageResult<Map<String, Object>> getMessages(Long conversationId, Long userId, Integer page, Integer size) {
        ChatConversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) throw new BusinessException("会话不存在");
        if (!conv.getUser1Id().equals(userId) && !conv.getUser2Id().equals(userId)) {
            throw new BusinessException("无权访问该会话");
        }

        // L3-M0-3：不再过滤已撤回消息（对齐微信/QQ：撤回=保留占位符，非移除）
        // 前端根据 deleted 字段渲染"你/对方撤回了一条消息"占位，此为行业主流设计。
        Page<ChatMessage> p = messageMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getConversationId, conversationId)
                        .orderByDesc(ChatMessage::getCreateTime));

        // 批量预加载发送者信息（一次查询代替每条消息各查一次）
        Set<Long> senderIds = p.getRecords().stream()
                .map(ChatMessage::getSenderId).collect(Collectors.toSet());
        Map<Long, SysUser> senderMap = senderIds.isEmpty() ? Collections.emptyMap() :
                userMapper.selectBatchIds(senderIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, u -> u));

        List<Map<String, Object>> records = p.getRecords().stream().map(msg -> {
            Map<String, Object> m = new LinkedHashMap<>();
            boolean isDeleted = msg.getDeletedAt() != null;
            m.put("id", msg.getId());
            m.put("senderId", msg.getSenderId());
            m.put("receiverId", msg.getReceiverId());
            // L3-M0-3：已撤回消息的 content 脱敏为 null，防止 Network Tab / 前端 devtools 泄漏原文
            // 前端根据 deleted 标记显示占位符，不依赖 content 字段
            m.put("content", isDeleted ? null : msg.getContent());
            // L3-M0-4：messageType 让前端区分文字/图片/文件，决定"重新编辑"按钮是否可用
            m.put("messageType", msg.getMessageType());
            m.put("isRead", msg.getIsRead());
            m.put("createTime", msg.getCreateTime());
            m.put("isMe", msg.getSenderId().equals(userId));
            // L3-M0-3：撤回元数据（与 WS MESSAGE_DELETED 事件载荷结构保持一致）
            m.put("deleted", isDeleted);
            if (isDeleted) {
                m.put("deletedAt", msg.getDeletedAt());
                m.put("deletedBy", msg.getDeletedBy());
            }
            SysUser sender = senderMap.get(msg.getSenderId());
            m.put("senderName", sender != null ? sender.getRealName() : "未知");
            m.put("senderAvatar", sender != null ? sender.getAvatar() : null);
            return m;
        }).collect(Collectors.toList());

        return new PageResult<>(records, p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Override
    public Map<String, Object> sendMessage(Long senderId, Long receiverId, String content) {
        return sendMessage(senderId, receiverId, content, null);
    }

    @Override
    @Transactional
    public Map<String, Object> sendMessage(Long senderId, Long receiverId, String content, String clientMsgId) {
        // ========== 1. 基础参数校验 ==========
        if (senderId.equals(receiverId)) throw new BusinessException("不能给自己发消息");
        if (content == null || content.trim().isEmpty()) throw new BusinessException("消息内容不能为空");

        // 规范化内容：trim 后存储，防止空白字符浪费空间
        content = content.trim();

        // ========== 2. 长度校验（按 Unicode code point，防 emoji 代理对绕过） ==========
        int cpCount = content.codePointCount(0, content.length());
        if (cpCount > ChatConstants.MAX_MESSAGE_LENGTH) {
            throw new BusinessException("消息内容不能超过 " + ChatConstants.MAX_MESSAGE_LENGTH + " 字符");
        }

        // ========== 2.5. 媒体消息 URL 协议白名单校验（防 XSS） ==========
        validateMediaContent(content);

        // ========== 3. 查询发送者（需要 roleId 做限流分级） ==========
        SysUser sender = userMapper.selectById(senderId);
        if (sender == null) throw new BusinessException("用户不存在");

        // ========== 4. 速率限制（按角色分级） ==========
        if (!rateLimiterService.tryAcquire(senderId, sender.getRoleId())) {
            throw new BusinessException("发送过于频繁，请稍后再试");
        }

        // ========== 5. 幂等原子占位（L3-M1-1：消除 check-then-act 竞态） ==========
        // 用 Redis SETNX 抢占 slot：抢到才继续插 DB；抢不到则说明有并发重试，尝试取已有结果。
        boolean acquiredSlot = idempotencyService.tryAcquireSlot(senderId, clientMsgId);
        if (!acquiredSlot) {
            // 有并发请求抢先占位。先查是否已完成，完成了直接返回已有消息（完美幂等）
            Long existingMsgId = idempotencyService.getExistingMessageId(senderId, clientMsgId);
            if (existingMsgId != null) {
                ChatMessage existing = messageMapper.selectById(existingMsgId);
                if (existing != null) {
                    return buildResultMap(existing, sender, clientMsgId);
                }
            }
            // 仍 PENDING：告知客户端稍后重试（避免两个请求同时插 DB 造成重复）
            throw new BusinessException("上一条消息正在处理中，请稍后重试");
        }

        // 抢到 slot 后的业务流程统一用 try/catch 包裹，任何异常都释放占位允许立即重试
        try {

        // ========== 6. 业务规则校验（L3-bugfix TYPING-001：委托单一事实源） ==========
        // 完整覆盖：receiver 存在/启用状态、学生考试中防作弊、角色矩阵（管理员/教师/学生互通约束）。
        // 与内联版完全等价；同时保证 ChatTypingController 走同一套规则，typing/sendMessage 永不漂移。
        permissionService.assertCanChat(senderId, receiverId);

        // ========== 7. 查找或创建会话 ==========
        // 按 code point 安全截取 lastMessage 预览（防止 emoji 代理对被切断）
        String preview = truncateByCodePoint(content, ChatConstants.LAST_MESSAGE_PREVIEW_LENGTH);

        Long user1 = Math.min(senderId, receiverId);
        Long user2 = Math.max(senderId, receiverId);
        ChatConversation conv = conversationMapper.selectOne(new LambdaQueryWrapper<ChatConversation>()
                .eq(ChatConversation::getUser1Id, user1).eq(ChatConversation::getUser2Id, user2));
        if (conv == null) {
            conv = new ChatConversation();
            conv.setUser1Id(user1);
            conv.setUser2Id(user2);
            conv.setLastMessage(preview);
            conv.setLastMessageTime(LocalDateTime.now());
            conv.setLastMessageSenderId(senderId);  // L3-M0-5
            conv.setUser1Hidden(0);
            conv.setUser2Hidden(0);
            conversationMapper.insert(conv);
        } else {
            conv.setLastMessage(preview);
            conv.setLastMessageTime(LocalDateTime.now());
            conv.setLastMessageSenderId(senderId);  // L3-M0-5：新消息发送者覆盖旧值
            // L3：有新消息时自动解除双方的"隐藏"状态，让会话重新回到列表
            conv.setUser1Hidden(0);
            conv.setUser2Hidden(0);
            conversationMapper.updateById(conv);
        }

        // ========== 8. 创建消息 ==========
        ChatMessage msg = new ChatMessage();
        msg.setConversationId(conv.getId());
        msg.setSenderId(senderId);
        msg.setReceiverId(receiverId);
        msg.setContent(content);
        msg.setMessageType(1);
        msg.setIsRead(0);
        msg.setCreateTime(LocalDateTime.now());
        messageMapper.insert(msg);

        // ========== 9. 记录幂等键 ==========
        // L3-M1-1：成功落盘后覆盖 PENDING 占位为真实 messageId，供并发重试取已有结果
        idempotencyService.markProcessed(senderId, clientMsgId, msg.getId());

        return buildResultMap(msg, sender, clientMsgId);

        } catch (RuntimeException e) {
            // L3-M1-1：业务失败时释放 slot，允许用户立即用同一 clientMsgId 重试
            // （否则会被卡 30 秒直到 Redis TTL 自动过期）。原异常继续抛出以保持事务回滚。
            idempotencyService.releaseSlot(senderId, clientMsgId);
            throw e;
        }
    }

    /**
     * L3-M3-1：原子批量发送（Slack chat.postMessage batch / Telegram sendMediaGroup / Discord 单消息多附件 架构）。
     * <p>
     * <b>实现要点：</b>
     * <ul>
     *   <li><b>非 @Transactional</b>：Partial Success 语义要求单条失败不能回滚其他条，每条独立写库</li>
     *   <li><b>共享校验前置</b>：sender/receiver 身份、角色权限、会话 upsert、考试中检测 —— 批次只执行 1 次</li>
     *   <li><b>批次级限流</b>：整批 1 permit（对齐 Slack，而非 N permit），根治"批量 5 文件第 3 条起被限"</li>
     *   <li><b>per-item 幂等</b>：每条独立 clientMsgId + Redis slot，重复批次自动返回 already_sent</li>
     * </ul>
     */
    @Override
    public List<Map<String, Object>> sendMessagesBatch(Long senderId, Long receiverId, List<Map<String, String>> items) {
        // ========== 1. 入口快速失败 ==========
        if (senderId == null || receiverId == null) {
            throw new BusinessException("发送者或接收者 ID 不能为空");
        }
        if (senderId.equals(receiverId)) {
            throw new BusinessException("不能给自己发消息");
        }
        if (items == null || items.isEmpty()) {
            throw new BusinessException("批次消息列表不能为空");
        }
        if (items.size() > ChatConstants.MAX_BATCH_SEND_SIZE) {
            throw new BusinessException("单批最多发送 " + ChatConstants.MAX_BATCH_SEND_SIZE + " 条消息");
        }

        // ========== 2. 查询发送者（含 roleId） ==========
        SysUser sender = userMapper.selectById(senderId);
        if (sender == null) throw new BusinessException("用户不存在");

        // ========== 3. 批次级速率限制（关键：整批 1 permit） ==========
        if (!rateLimiterService.tryAcquire(senderId, sender.getRoleId())) {
            throw new BusinessException("发送过于频繁，请稍后再试");
        }

        // ========== 4. 查询接收者 + 基础业务校验 ==========
        SysUser receiver = userMapper.selectById(receiverId);
        if (receiver == null) throw new BusinessException("用户不存在");
        if (receiver.getStatus() == null || receiver.getStatus() != 1) {
            throw new BusinessException("对方账号已被禁用，无法发送消息");
        }

        // ========== 4-5. 业务规则 + 角色矩阵（L3-bugfix TYPING-001：委托单一事实源） ==========
        // 与单条 sendMessage 完全一致；同时供 ChatTypingController 复用，typing/sendMessage 永不漂移。
        // 覆盖：学生考试中防作弊、角色矩阵（管理员通配、教师→学生班级约束、学生↔学生禁止、学生→教师班级约束）。
        permissionService.assertCanChat(senderId, receiverId);

        // ========== 6. 查找或创建会话（共享一次 upsert） ==========
        Long user1 = Math.min(senderId, receiverId);
        Long user2 = Math.max(senderId, receiverId);
        ChatConversation conv = conversationMapper.selectOne(new LambdaQueryWrapper<ChatConversation>()
                .eq(ChatConversation::getUser1Id, user1).eq(ChatConversation::getUser2Id, user2));
        if (conv == null) {
            conv = new ChatConversation();
            conv.setUser1Id(user1);
            conv.setUser2Id(user2);
            conv.setLastMessage("");
            conv.setLastMessageTime(LocalDateTime.now());
            conv.setLastMessageSenderId(senderId);
            conv.setUser1Hidden(0);
            conv.setUser2Hidden(0);
            conversationMapper.insert(conv);
        }

        // ========== 7. 逐条处理（Partial Success） ==========
        List<Map<String, Object>> results = new ArrayList<>(items.size());
        String lastSuccessContent = null;

        for (Map<String, String> item : items) {
            Map<String, Object> itemResult = new LinkedHashMap<>();
            String clientMsgId = item.get("clientMsgId");
            String content = item.get("content");
            itemResult.put("clientMsgId", clientMsgId);

            try {
                // 7.1 内容校验
                if (content == null || content.trim().isEmpty()) {
                    throw new BusinessException("消息内容不能为空");
                }
                content = content.trim();
                int cpCount = content.codePointCount(0, content.length());
                if (cpCount > ChatConstants.MAX_MESSAGE_LENGTH) {
                    throw new BusinessException("消息内容不能超过 " + ChatConstants.MAX_MESSAGE_LENGTH + " 字符");
                }
                validateMediaContent(content);

                // 7.2 独立幂等抢占
                boolean acquiredSlot = idempotencyService.tryAcquireSlot(senderId, clientMsgId);
                if (!acquiredSlot) {
                    Long existingMsgId = idempotencyService.getExistingMessageId(senderId, clientMsgId);
                    if (existingMsgId != null) {
                        ChatMessage existing = messageMapper.selectById(existingMsgId);
                        if (existing != null) {
                            itemResult.put("status", "already_sent");
                            itemResult.put("message", buildResultMap(existing, sender, clientMsgId));
                            results.add(itemResult);
                            continue;
                        }
                    }
                    itemResult.put("status", "failed");
                    itemResult.put("error", "上一条消息正在处理中，请稍后重试");
                    results.add(itemResult);
                    continue;
                }

                // 7.3 插入消息（messageMapper.insert 单条原子）
                ChatMessage msg = new ChatMessage();
                msg.setConversationId(conv.getId());
                msg.setSenderId(senderId);
                msg.setReceiverId(receiverId);
                msg.setContent(content);
                msg.setMessageType(1);
                msg.setIsRead(0);
                msg.setCreateTime(LocalDateTime.now());
                messageMapper.insert(msg);

                // 7.4 写入幂等完成标记
                idempotencyService.markProcessed(senderId, clientMsgId, msg.getId());

                lastSuccessContent = content;
                itemResult.put("status", "sent");
                itemResult.put("message", buildResultMap(msg, sender, clientMsgId));
            } catch (RuntimeException e) {
                // 单条失败：释放 slot，不阻塞批次
                idempotencyService.releaseSlot(senderId, clientMsgId);
                itemResult.put("status", "failed");
                itemResult.put("error", e.getMessage() != null ? e.getMessage() : "发送失败");
            }
            results.add(itemResult);
        }

        // ========== 8. 批次结束后更新会话预览（仅当至少有 1 条成功时） ==========
        if (lastSuccessContent != null) {
            String preview = truncateByCodePoint(lastSuccessContent, ChatConstants.LAST_MESSAGE_PREVIEW_LENGTH);
            conv.setLastMessage(preview);
            conv.setLastMessageTime(LocalDateTime.now());
            conv.setLastMessageSenderId(senderId);
            conv.setUser1Hidden(0);
            conv.setUser2Hidden(0);
            conversationMapper.updateById(conv);
        }

        return results;
    }

    /**
     * 构建 WebSocket 推送所需的消息 Map。
     *
     * @param msg         已落库的消息实体
     * @param sender      发送者实体（用于携带显示名 / 头像，避免前端二次查询）
     * @param clientMsgId 客户端幂等键（可能为 null：管理员强删等非用户发起场景）
     *                    <p>L3-M1-3：随结果返回，用于前端在"REST 响应 + WS 自回推"双路径下做去重。
     *                    稳定标识属性：前端生成、贯穿乐观占位 → REST 响应 → WS 推送三个阶段，
     *                    替代以往依赖后端 id 的去重方案（后端 id 在乐观占位阶段尚未产生，去重必然失败）。
     *                    业界同构实现：Zulip local_id、Baileys messageKey.id、Signal clientMessageId。
     */
    private Map<String, Object> buildResultMap(ChatMessage msg, SysUser sender, String clientMsgId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", msg.getId());
        result.put("conversationId", msg.getConversationId());
        result.put("senderId", msg.getSenderId());
        result.put("receiverId", msg.getReceiverId());
        result.put("content", msg.getContent());
        result.put("createTime", msg.getCreateTime());
        result.put("senderName", sender.getRealName());
        result.put("senderAvatar", sender.getAvatar());
        if (clientMsgId != null) {
            result.put("clientMsgId", clientMsgId);
        }
        return result;
    }

    /**
     * 按 Unicode code point 安全截取字符串（emoji 代理对不会被切断）
     */
    private static String truncateByCodePoint(String s, int maxCodePoints) {
        if (s == null) return null;
        int cpCount = s.codePointCount(0, s.length());
        if (cpCount <= maxCodePoints) return s;
        int endIdx = s.offsetByCodePoints(0, maxCodePoints);
        return s.substring(0, endIdx);
    }

    // ========== 媒体消息 URL 校验（防 XSS） ==========
    /** 单图消息：[img]URL[/img] */
    private static final Pattern IMG_SINGLE_PATTERN = Pattern.compile("^\\[img](.+)\\[/img]$", Pattern.DOTALL);
    /** 多图消息：[imgs]URL1||URL2[/imgs] */
    private static final Pattern IMG_MULTI_PATTERN = Pattern.compile("^\\[imgs](.+)\\[/imgs]$", Pattern.DOTALL);
    /** 文件消息：[file:ext:name]URL[/file] */
    private static final Pattern FILE_PATTERN = Pattern.compile("^\\[file:\\w+:.+?](.+)\\[/file]$", Pattern.DOTALL);

    /**
     * 校验消息内容中的媒体 URL 是否合规（防 XSS）。
     * <p>
     * 匹配三类媒体标记，对其中的 URL 做协议白名单校验：
     * <ul>
     *   <li>{@code [img]URL[/img]}</li>
     *   <li>{@code [imgs]URL1||URL2[/imgs]}</li>
     *   <li>{@code [file:ext:name]URL[/file]}</li>
     * </ul>
     * 纯文本消息（不匹配任何媒体标记）跳过此校验，由前端 {@code {{}}} 插值默认转义提供 XSS 防护。
     * <p>
     * 参考：OWASP XSS Prevention Cheat Sheet §4 - URL Contexts
     */
    private void validateMediaContent(String content) {
        Matcher m;
        if ((m = IMG_SINGLE_PATTERN.matcher(content)).matches()) {
            validateUrlScheme(m.group(1));
        } else if ((m = IMG_MULTI_PATTERN.matcher(content)).matches()) {
            for (String url : m.group(1).split("\\|\\|")) {
                validateUrlScheme(url);
            }
        } else if ((m = FILE_PATTERN.matcher(content)).matches()) {
            validateUrlScheme(m.group(1));
        }
    }

    /**
     * URL 协议白名单（http/https/站内相对路径/协议相对 URL），其他一律拒绝。
     */
    private void validateUrlScheme(String url) {
        if (url == null) {
            throw new BusinessException("消息中包含无效的链接");
        }
        String trimmed = url.trim();
        if (trimmed.isEmpty()) {
            throw new BusinessException("消息中包含无效的链接");
        }
        // 白名单 1：http:// 或 https://（大小写不敏感）
        if (trimmed.length() >= 7
                && (trimmed.regionMatches(true, 0, "http://", 0, 7)
                    || (trimmed.length() >= 8 && trimmed.regionMatches(true, 0, "https://", 0, 8)))) {
            return;
        }
        // 白名单 2：站内相对路径（/xxx、./xxx、../xxx）
        if (trimmed.charAt(0) == '/' || trimmed.startsWith("./") || trimmed.startsWith("../")) {
            return;
        }
        // 其他一律拒绝（javascript: / data: / vbscript: / file: / ftp: / mailto: 等）
        throw new BusinessException("消息中包含不允许的链接协议");
    }

    @Override
    public void markAsRead(Long conversationId, Long userId) {
        // 归属校验（与 getMessages 保持一致，消除 IDOR 探测边缘）：
        // 1. 会话必须存在
        // 2. 当前用户必须是会话的双方之一
        ChatConversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) {
            throw new BusinessException("会话不存在");
        }
        if (!conv.getUser1Id().equals(userId) && !conv.getUser2Id().equals(userId)) {
            throw new BusinessException("无权访问该会话");
        }
        int updated = messageMapper.update(null, new LambdaUpdateWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversationId)
                .eq(ChatMessage::getReceiverId, userId)
                .eq(ChatMessage::getIsRead, 0)
                .set(ChatMessage::getIsRead, 1));

        // M7：已读回执 WS 回推给对方
        // L3-M1-1：同时回推一份 SELF_READ 给自己，让同账号其他标签页同步 unread 清零
        // 仅在有消息实际被标为已读时推送，避免无谓流量
        if (updated > 0) {
            Long otherUserId = conv.getUser1Id().equals(userId) ? conv.getUser2Id() : conv.getUser1Id();
            String nowStr = LocalDateTime.now().toString();

            // 推给对方：READ_RECEIPT（原有行为）
            Map<String, Object> receiptForOther = new LinkedHashMap<>();
            receiptForOther.put("type", "READ_RECEIPT");
            receiptForOther.put("conversationId", conversationId);
            receiptForOther.put("readerId", userId);
            receiptForOther.put("readAt", nowStr);
            receiptForOther.put("count", updated);
            try {
                messagingTemplate.convertAndSendToUser(
                        String.valueOf(otherUserId),
                        "/queue/read-receipts",
                        receiptForOther);
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(ChatServiceImpl.class)
                        .warn("推送已读回执失败: conversationId={}, otherUserId={}", conversationId, otherUserId, e);
            }

            // L3-M1-1 新增：推给自己所有 session（多标签页同步）
            // Spring user destination 按 userId 广播到该用户所有活跃 WebSocket session，
            // 前端 handleReadReceipt 识别 type='SELF_READ' 后清零 conv.unreadCount + refresh 全局未读
            Map<String, Object> receiptForSelf = new LinkedHashMap<>();
            receiptForSelf.put("type", "SELF_READ");
            receiptForSelf.put("conversationId", conversationId);
            receiptForSelf.put("readerId", userId);
            receiptForSelf.put("readAt", nowStr);
            receiptForSelf.put("count", updated);
            try {
                messagingTemplate.convertAndSendToUser(
                        String.valueOf(userId),
                        "/queue/read-receipts",
                        receiptForSelf);
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(ChatServiceImpl.class)
                        .warn("推送 SELF_READ 失败: conversationId={}, userId={}", conversationId, userId, e);
            }
        }
    }

    @Override
    public Map<String, Object> getUnreadCount(Long userId) {
        // L3-M1 修复：过滤已软删消息，与 countUnreadByConversations（单会话未读统计）保持一致。
        // L3-M1-2 修复：同步排除"用户主动隐藏"的会话，避免"铃铛 +N 却在会话列表里找不到"的幽灵未读。
        // 语义：用户隐藏会话 = 不想被打扰 → 未读数也不应通过铃铛提示（与 getConversations 过滤行为对称）
        List<Long> hiddenConvIds = conversationMapper.selectList(
                new LambdaQueryWrapper<ChatConversation>()
                        .select(ChatConversation::getId)
                        .and(w -> w.and(w1 -> w1.eq(ChatConversation::getUser1Id, userId)
                                                .eq(ChatConversation::getUser1Hidden, 1))
                                    .or(w1 -> w1.eq(ChatConversation::getUser2Id, userId)
                                                .eq(ChatConversation::getUser2Hidden, 1)))
        ).stream().map(ChatConversation::getId).collect(Collectors.toList());

        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getReceiverId, userId)
                .eq(ChatMessage::getIsRead, 0)
                .isNull(ChatMessage::getDeletedAt);
        if (!hiddenConvIds.isEmpty()) {
            wrapper.notIn(ChatMessage::getConversationId, hiddenConvIds);
        }
        long total = messageMapper.selectCount(wrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        return result;
    }

    /**
     * I2：WS 重连补拉。
     * <p>
     * 实现要点：
     * <ol>
     *   <li>仅返回 <b>本人作为 sender 或 receiver</b> 的消息，防止跨用户越权拉取</li>
     *   <li>limit 服务端约束到 [1, 500]，即使客户端传入超大值也不会放大 DB 压力</li>
     *   <li>按 id 升序返回，客户端 foreach 时可保证稳定顺序 + 最后一条 id 即为新的 sinceMessageId</li>
     *   <li>复用 getMessages 的 batch load sender 思路，避免 N+1</li>
     *   <li>返回 Map 结构与 WS 推送一致（含 conversationId），前端可直接走 handleIncomingMessage 去重合并</li>
     * </ol>
     */
    @Override
    public List<Map<String, Object>> getIncrementalMessages(Long userId, Long sinceMessageId, Integer limit) {
        if (userId == null) return Collections.emptyList();
        // 服务端硬约束：单次 1..500，防止放大攻击
        int safeLimit = (limit == null || limit <= 0) ? 200 : Math.min(limit, 500);
        long since = sinceMessageId == null ? 0L : sinceMessageId;

        // 使用 QueryWrapper 表达 id > since AND (sender = me OR receiver = me)
        // OR 条件用嵌套 and(..or..) 包裹，避免与未来扩展的 AND 条件优先级冲突（参考 M3 修复）
        // L3-M0-2 修复：不再过滤 deleted_at —— 重连补拉必须把断连期间的软删状态一并带回，
        // 否则客户端本地已渲染的消息在对方撤回后永远停留在旧状态（因为 MESSAGE_DELETED WS 事件在断连期间已丢失）。
        // 前端 handleIncomingMessage 收到 deletedAt 非空的消息时会转换为 MESSAGE_DELETED 事件处理。
        List<ChatMessage> records = messageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .gt(ChatMessage::getId, since)
                .and(w -> w.eq(ChatMessage::getSenderId, userId)
                           .or().eq(ChatMessage::getReceiverId, userId))
                .orderByAsc(ChatMessage::getId)
                .last("LIMIT " + safeLimit));

        if (records.isEmpty()) return Collections.emptyList();

        // 批量预加载 sender（避免 N+1）
        Set<Long> senderIds = records.stream().map(ChatMessage::getSenderId).collect(Collectors.toSet());
        Map<Long, SysUser> senderMap = senderIds.isEmpty() ? Collections.emptyMap() :
                userMapper.selectBatchIds(senderIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, u -> u));

        return records.stream().map(msg -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", msg.getId());
            m.put("conversationId", msg.getConversationId()); // ⭐ 必须带上，前端按此字段归档到会话
            m.put("senderId", msg.getSenderId());
            m.put("receiverId", msg.getReceiverId());
            m.put("content", msg.getContent());
            m.put("isRead", msg.getIsRead());
            m.put("createTime", msg.getCreateTime());
            m.put("isMe", msg.getSenderId().equals(userId));
            // L3-M0-2：带上软删字段，前端据此在断线重连后补齐 MESSAGE_DELETED 事件
            m.put("deletedAt", msg.getDeletedAt());
            m.put("deletedBy", msg.getDeletedBy());
            SysUser sender = senderMap.get(msg.getSenderId());
            m.put("senderName", sender != null ? sender.getRealName() : "未知");
            m.put("senderAvatar", sender != null ? sender.getAvatar() : null);
            return m;
        }).collect(Collectors.toList());
    }

    // ========== L3：消息撤回 / 删除 / 会话隐藏 ==========

    private static final org.slf4j.Logger L3_LOG = org.slf4j.LoggerFactory.getLogger(ChatServiceImpl.class);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recallMessage(Long messageId, Long userId) {
        if (messageId == null || userId == null) {
            throw new BusinessException("参数错误");
        }
        ChatMessage msg = messageMapper.selectById(messageId);
        if (msg == null) throw new BusinessException("消息不存在");
        if (msg.getDeletedAt() != null) throw new BusinessException("消息已被删除");
        if (!userId.equals(msg.getSenderId())) {
            throw new BusinessException("只能撤回自己发送的消息");
        }
        // 时限校验：超过 MESSAGE_RECALL_WINDOW_SECONDS 后不允许撤回
        LocalDateTime limit = LocalDateTime.now().minusSeconds(ChatConstants.MESSAGE_RECALL_WINDOW_SECONDS);
        if (msg.getCreateTime() == null || msg.getCreateTime().isBefore(limit)) {
            throw new BusinessException("消息发送超过 "
                    + (ChatConstants.MESSAGE_RECALL_WINDOW_SECONDS / 60) + " 分钟，不能撤回");
        }
        // L3-M0-4：撤回前保存文字消息原文到 2 分钟草稿缓存，供"重新编辑"回填。
        // messageType=1 表示文字；图片/文件/语音不保存（与微信对齐 — 非文字消息无法"重新编辑"）。
        if (Integer.valueOf(1).equals(msg.getMessageType()) && msg.getContent() != null) {
            recallDraftService.saveDraft(msg.getId(), msg.getContent());
        }
        softDeleteMessage(msg, userId);
        // L3-M0-1：撤回可能是会话最后一条，重算 conversation.lastMessage 避免预览残留已撤回内容
        ChatConversation updatedConv = refreshConversationLastMessage(msg.getConversationId());
        broadcastMessageDeleted(msg, userId, updatedConv);
    }

    /**
     * L3-M0-4：获取撤回消息草稿，供发送者"重新编辑"回填使用。
     * <p>
     * <b>安全要点</b>：撤回前的原文属于隐私 — 只有发送者本人能取；接收者、管理员、第三方调用均拒绝。
     * 草稿 TTL = 2min（与 RECALL_DRAFT_TTL_SECONDS 同值），过期后原文彻底不可恢复。
     */
    @Override
    public String getRecallDraft(Long messageId, Long userId) {
        if (messageId == null || userId == null) {
            throw new BusinessException("参数错误");
        }
        ChatMessage msg = messageMapper.selectById(messageId);
        if (msg == null) throw new BusinessException("消息不存在");
        // 只有原发送者能拿自己的撤回草稿（接收者/管理员都不行，防止偷看原文）
        if (!userId.equals(msg.getSenderId())) {
            throw new BusinessException("只能重新编辑自己的消息");
        }
        // 未撤回的消息没有草稿（逻辑上不应该被查）
        if (msg.getDeletedAt() == null) {
            throw new BusinessException("消息未撤回");
        }
        // 非文字消息的撤回根本没有保存草稿，这里直接返回 null（前端隐藏按钮即可）
        if (!Integer.valueOf(1).equals(msg.getMessageType())) {
            return null;
        }
        // TTL 过期返回 null；前端据此隐藏"重新编辑"按钮
        return recallDraftService.getDraft(messageId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminDeleteMessage(Long messageId, Long adminId) {
        if (messageId == null || adminId == null) {
            throw new BusinessException("参数错误");
        }
        SysUser admin = userMapper.selectById(adminId);
        if (admin == null || !RoleConstants.ADMIN_ROLE_ID.equals(admin.getRoleId())) {
            throw new BusinessException("仅管理员可强制删除消息");
        }
        ChatMessage msg = messageMapper.selectById(messageId);
        if (msg == null) throw new BusinessException("消息不存在");
        if (msg.getDeletedAt() != null) throw new BusinessException("消息已被删除");
        softDeleteMessage(msg, adminId);
        L3_LOG.warn("[AUDIT] 管理员强删消息: adminId={}, messageId={}, senderId={}, receiverId={}, conversationId={}",
                adminId, msg.getId(), msg.getSenderId(), msg.getReceiverId(), msg.getConversationId());
        // L3-M0-1：强删也可能命中会话最后一条，同步重算预览
        ChatConversation updatedConv = refreshConversationLastMessage(msg.getConversationId());
        broadcastMessageDeleted(msg, adminId, updatedConv);
    }

    @Override
    public void hideConversation(Long conversationId, Long userId) {
        if (conversationId == null || userId == null) {
            throw new BusinessException("参数错误");
        }
        ChatConversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) throw new BusinessException("会话不存在");
        if (!userId.equals(conv.getUser1Id()) && !userId.equals(conv.getUser2Id())) {
            throw new BusinessException("无权操作该会话");
        }
        LambdaUpdateWrapper<ChatConversation> update = new LambdaUpdateWrapper<ChatConversation>()
                .eq(ChatConversation::getId, conversationId);
        if (userId.equals(conv.getUser1Id())) {
            update.set(ChatConversation::getUser1Hidden, 1);
        } else {
            update.set(ChatConversation::getUser2Hidden, 1);
        }
        conversationMapper.update(null, update);

        // M2：多标签页同步（与 markAsRead 的 SELF_READ / 撤回事件的 MESSAGE_DELETED 同模式）
        // 复用 /queue/message-events 通道，前端 handleMessageEvent 识别 CONVERSATION_HIDDEN 后：
        //   1. 从 conversations 列表里移除该会话
        //   2. 若当前正在打开该会话，清除当前状态
        //   3. 刷新全局铃铛（隐藏后该会话的未读不再计入）
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "CONVERSATION_HIDDEN");
        event.put("conversationId", conversationId);
        try {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(userId),
                    "/queue/message-events",
                    event);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(ChatServiceImpl.class)
                    .warn("推送 CONVERSATION_HIDDEN 失败: conversationId={}, userId={}", conversationId, userId, e);
        }
    }

    /**
     * L3-M1-4：取消归档实现（对称 {@link #hideConversation}）。
     * <p>
     * 与 hideConversation 完全镜像：同样的权限校验、per-user 字段更新、WS 多标签页同步。
     */
    @Override
    public void unhideConversation(Long conversationId, Long userId) {
        if (conversationId == null || userId == null) {
            throw new BusinessException("参数错误");
        }
        ChatConversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) throw new BusinessException("会话不存在");
        if (!userId.equals(conv.getUser1Id()) && !userId.equals(conv.getUser2Id())) {
            throw new BusinessException("无权操作该会话");
        }
        LambdaUpdateWrapper<ChatConversation> update = new LambdaUpdateWrapper<ChatConversation>()
                .eq(ChatConversation::getId, conversationId);
        if (userId.equals(conv.getUser1Id())) {
            update.set(ChatConversation::getUser1Hidden, 0);
        } else {
            update.set(ChatConversation::getUser2Hidden, 0);
        }
        conversationMapper.update(null, update);

        // 多标签页同步：前端 handleMessageEvent 识别 CONVERSATION_UNHIDDEN 后：
        //   1. 从归档列表移除该会话
        //   2. 触发主列表刷新（拉取 getConversations 以获取完整的最新状态）
        //   3. 刷新全局铃铛（会话重新可见后未读重新计入）
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "CONVERSATION_UNHIDDEN");
        event.put("conversationId", conversationId);
        try {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(userId),
                    "/queue/message-events",
                    event);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(ChatServiceImpl.class)
                    .warn("推送 CONVERSATION_UNHIDDEN 失败: conversationId={}, userId={}", conversationId, userId, e);
        }
    }

    /**
     * L3 内部工具：软删消息，写入 deleted_at / deleted_by。
     */
    private void softDeleteMessage(ChatMessage msg, Long operatorId) {
        messageMapper.update(null, new LambdaUpdateWrapper<ChatMessage>()
                .eq(ChatMessage::getId, msg.getId())
                .set(ChatMessage::getDeletedAt, LocalDateTime.now())
                .set(ChatMessage::getDeletedBy, operatorId));
    }

    /**
     * L3-M0-1 / L3-M0-3：重算指定会话的 lastMessage / lastMessageTime 预览。
     * <p>
     * 在消息被撤回/强删后调用，刷新会话列表预览。
     * <p>
     * <b>业界对齐（微信/QQ/WhatsApp/Signal）</b>：撤回不等于会话消失。即使所有消息都被撤回，
     * 会话预览也必须显示"此消息已撤回"占位符（而非"暂无消息"或留空）— 否则用户会认为
     * "会话被清空了"，与其心理预期严重不符。
     * <p>
     * 策略：查询时**不再**过滤 deleted_at，取物理上的最新一条：
     * <ul>
     *     <li>该条未撤回 → 预览 = content 截断</li>
     *     <li>该条已撤回 → 预览 = DELETED_MESSAGE_PLACEHOLDER，时间戳保留</li>
     *     <li>整个会话无任何消息（新建会话且无发消息） → 预览 = null（这是唯一合理的空态）</li>
     * </ul>
     *
     * @return 更新后的会话实体，供 WS 广播携带新预览给前端
     */
    private ChatConversation refreshConversationLastMessage(Long conversationId) {
        List<ChatMessage> latest = messageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getConversationId, conversationId)
                        .orderByDesc(ChatMessage::getCreateTime)
                        .last("LIMIT 1"));
        LambdaUpdateWrapper<ChatConversation> update = new LambdaUpdateWrapper<ChatConversation>()
                .eq(ChatConversation::getId, conversationId);
        if (latest.isEmpty()) {
            // 会话中从未有过任何消息（极罕见：新建会话且双方都没发过）→ 合理的空态
            update.set(ChatConversation::getLastMessage, null)
                  .set(ChatConversation::getLastMessageTime, null)
                  .set(ChatConversation::getLastMessageSenderId, null);
        } else {
            ChatMessage next = latest.get(0);
            String preview;
            if (next.getDeletedAt() != null) {
                // L3-M0-3：撤回消息用占位符作预览，保持"会话有活动"的感知
                preview = ChatConstants.DELETED_MESSAGE_PLACEHOLDER;
            } else {
                preview = truncateByCodePoint(next.getContent(), ChatConstants.LAST_MESSAGE_PREVIEW_LENGTH);
            }
            update.set(ChatConversation::getLastMessage, preview)
                  .set(ChatConversation::getLastMessageTime, next.getCreateTime())
                  // L3-M0-5：同步写入 sender_id，前端据此渲染"你/对方撤回了一条消息"
                  .set(ChatConversation::getLastMessageSenderId, next.getSenderId());
        }
        conversationMapper.update(null, update);
        return conversationMapper.selectById(conversationId);
    }

    /**
     * L3 内部工具：通过 WS 广播 MESSAGE_DELETED 事件给会话双方。
     * 前端订阅 /user/queue/message-events，收到后把对应消息替换为"已撤回"占位。
     * <p>
     * L3-M0-1：载荷携带 newLastMessage / newLastMessageTime，前端可同步更新会话列表预览，
     * 无需额外调用 loadConversations 做全量拉取。
     */
    private void broadcastMessageDeleted(ChatMessage msg, Long operatorId, ChatConversation updatedConv) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "MESSAGE_DELETED");
        payload.put("messageId", msg.getId());
        payload.put("conversationId", msg.getConversationId());
        payload.put("deletedBy", operatorId);
        payload.put("deletedAt", LocalDateTime.now().toString());
        if (updatedConv != null) {
            // 即使 null 也要显式放入，前端据此判断是否需要清空预览
            payload.put("newLastMessage", updatedConv.getLastMessage());
            payload.put("newLastMessageTime", updatedConv.getLastMessageTime());
            // L3-M0-5：实时同步 sender_id，前端 WS 路径与 REST 路径预览文案渲染一致
            payload.put("newLastMessageSenderId", updatedConv.getLastMessageSenderId());
        }
        for (Long target : new Long[]{msg.getSenderId(), msg.getReceiverId()}) {
            if (target == null) continue;
            try {
                messagingTemplate.convertAndSendToUser(
                        String.valueOf(target),
                        "/queue/message-events",
                        payload);
            } catch (Exception e) {
                L3_LOG.warn("推送 MESSAGE_DELETED 失败: targetUserId={}, messageId={}", target, msg.getId(), e);
            }
        }
    }
}
