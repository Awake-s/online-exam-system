package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.common.constants.RoleConstants;
import com.exam.common.result.PageResult;
import com.exam.entity.*;
import com.exam.mapper.*;
import com.exam.service.NotificationActionUrlResolver;
import com.exam.service.NotificationOptions;
import com.exam.service.NotificationService;
import com.exam.service.NotificationPushService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 通知服务实现类 - E1 架构升级版
 * <p>
 * <b>历史优化点</b>：
 * <ol>
 *   <li>批量插入替代逐条插入</li>
 *   <li>WebSocket 实时推送</li>
 *   <li>待办查询性能优化（避免 N+1）</li>
 * </ol>
 * <p>
 * <b>E1 新增能力</b>：
 * <ol>
 *   <li><b>优先级</b>：通知分级（1=紧急 / 2=普通 / 3=次要），UI 可差异化渲染</li>
 *   <li><b>发送者信息</b>：payload 携带 senderId/senderName/senderAvatar，支持 Slack 风格头像展示</li>
 *   <li><b>action_url 后端生成</b>：点击跳转路径由后端计算，前端 {@code useNotificationHandler} 从 60 行硬编码收敛至通用逻辑</li>
 *   <li><b>payload JSON 扩展</b>：所有未来富内容/结构化数据通过 payload 承载，零额外改表</li>
 * </ol>
 *
 * <p><b>事务传播约束（REQUIRES_NEW）</b>：本服务所有公开发送方法均使用
 * {@link Propagation#REQUIRES_NEW}。该决策依据 Spring 官方 Javadoc「§TransactionSynchronization.afterCommit」明示：
 * <i>“Use PROPAGATION_REQUIRES_NEW for any transactional operation that is called from here.”</i>。
 * <ul>
 *   <li>✅ 推荐调用场景：主业务 {@code afterCommit()} 回调中（项目所有 Service 已采用此模式）</li>
 *   <li>✅ 推荐调用场景：无事务上下文（定时任务、异步事件）</li>
 *   <li>⚠ 禁止调用场景：主业务事务内部直接调用——会导致“通知先提交、主业务后回滚”的数据不一致</li>
 * </ul>
 * <p>参考：Spring Framework 5.3.x source · {@code AbstractPlatformTransactionManager#triggerAfterCommit}、
 * GitHub Issue spring-projects/spring-framework#26384。
 */
@Slf4j
@Service
public class NotificationServiceImpl extends ServiceImpl<SysNotificationMapper, SysNotification>
        implements NotificationService {

    @Autowired private SysNotificationMapper notificationMapper;
    @Autowired private SysUserMapper userMapper;
    @Autowired private ExamExamMapper examMapper;
    @Autowired private ExamRecordMapper recordMapper;
    @Autowired private NotificationPushService pushService;
    @Autowired private ObjectMapper objectMapper; // Jackson 由 Spring Boot 自动装配

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** TypeReference 缓存：避免每次反序列化时反复创建 */
    private static final TypeReference<Map<String, Object>> PAYLOAD_MAP_TYPE = new TypeReference<Map<String, Object>>() {};

    // ===================== E1 辅助方法 =====================

    /**
     * 将 roleId 映射为 roleCode（ADMIN/TEACHER/STUDENT），用于 action_url 解析。
     * 未知角色返回 null。
     */
    private String mapRoleIdToCode(Long roleId) {
        if (roleId == null) return null;
        if (RoleConstants.ADMIN_ROLE_ID.equals(roleId)) return RoleConstants.ADMIN_CODE;
        if (RoleConstants.TEACHER_ROLE_ID.equals(roleId)) return RoleConstants.TEACHER_CODE;
        if (RoleConstants.STUDENT_ROLE_ID.equals(roleId)) return RoleConstants.STUDENT_CODE;
        return null;
    }

    /**
     * 为通知实体注入 priority 和 payload（若 options 含数据则序列化为 JSON 存入）。
     * <p>自动补全 action_url：options 未显式传入 actionUrl 时，尝试通过 resolver 生成。
     *
     * @param n                   待填充的通知实体
     * @param options             扩展选项（可为 null，则全部走默认值）
     * @param receiverRoleCode    接收者角色（用于 action_url 解析，可为 null）
     */
    private void applyOptions(SysNotification n, NotificationOptions options, String receiverRoleCode) {
        NotificationOptions opts = options != null ? options : NotificationOptions.defaults();

        // 1. 优先级
        n.setPriority(opts.getPriority());

        // 2. 自动补全 action_url（若 options 未显式传入）
        if (opts.getActionUrl() == null && receiverRoleCode != null) {
            String autoUrl = NotificationActionUrlResolver.resolve(
                    receiverRoleCode, n.getType(), n.getBizType(), n.getBizId());
            if (autoUrl != null) {
                opts.withActionUrl(autoUrl);
            }
        }

        // 3. 序列化 payload（仅当有实际数据时才写入，保持数据库紧凑）
        if (opts.hasPayloadData()) {
            try {
                Map<String, Object> payloadMap = opts.buildPayloadMap();
                n.setPayload(objectMapper.writeValueAsString(payloadMap));
            } catch (Exception e) {
                // 序列化失败不影响通知主流程
                log.warn("payload 序列化失败，类型: {}，bizId: {}", n.getType(), n.getBizId(), e);
            }
        }
    }

    /**
     * 将 payload JSON 字符串反序列化为 Map；失败返回 null。
     */
    private Map<String, Object> deserializePayload(String payload) {
        if (payload == null || payload.isEmpty()) return null;
        try {
            return objectMapper.readValue(payload, PAYLOAD_MAP_TYPE);
        } catch (Exception e) {
            log.warn("payload 反序列化失败: {}", payload, e);
            return null;
        }
    }

    // ===================== 发送通知方法 =====================

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void notifyUser(Long userId, String type, String title, String content, String bizType, Long bizId) {
        // 基础版：委托给扩展版，使用默认 options（priority=2 普通，无 sender，payload 为空）
        // 注：此处 this 自调用不走代理，重载 @Transactional 不生效；
        // 本方法顶层的 REQUIRES_NEW 已创建新事务 T2，insert 在 T2 内执行即可
        notifyUser(userId, type, title, content, bizType, bizId, null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void notifyUser(Long userId, String type, String title, String content, String bizType, Long bizId,
                           NotificationOptions options) {
        // 1. 构建实体
        SysNotification n = new SysNotification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setContent(content);
        n.setBizType(bizType);
        n.setBizId(bizId);
        n.setIsRead(0);

        // 2. 解析接收者角色用于 action_url 生成（若 options 中未显式提供）
        String receiverRoleCode = null;
        if (options == null || options.getActionUrl() == null) {
            SysUser receiver = userMapper.selectById(userId);
            if (receiver != null) {
                receiverRoleCode = mapRoleIdToCode(receiver.getRoleId());
            }
        }

        // 3. E1: 填充 priority + payload
        applyOptions(n, options, receiverRoleCode);

        // 4. 持久化
        notificationMapper.insert(n);

        // 5. 事务提交后再异步推送 WebSocket 通知
        // （如调用方在事务中，仅在事务提交后推送，避免推送早于事务提交的竞态）
        pushAfterCommitOrNow("notifyUser type=" + type + " userId=" + userId, () -> {
            try {
                Map<String, Object> pushData = buildPushData(n);
                pushService.pushToUser(userId, pushData);
            } catch (Exception e) {
                log.error("推送通知失败，用户ID: {}, 类型: {}", userId, type, e);
            }
        });
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void notifyUserWithDedupe(Long userId, String type, String title, String content,
                                      String bizType, Long bizId, int dedupeWindowMinutes) {
        // 同 notifyUser：此处 this 自调用不走代理，顶层 REQUIRES_NEW 已创建 T2
        notifyUserWithDedupe(userId, type, title, content, bizType, bizId, dedupeWindowMinutes, null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void notifyUserWithDedupe(Long userId, String type, String title, String content,
                                      String bizType, Long bizId, int dedupeWindowMinutes,
                                      NotificationOptions options) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.minusMinutes(dedupeWindowMinutes);

        // 查找时间窗口内最近一条同 user+type+bizType+bizId 的通知
        SysNotification existing = notificationMapper.selectOne(
                new LambdaQueryWrapper<SysNotification>()
                        .eq(SysNotification::getUserId, userId)
                        .eq(SysNotification::getType, type)
                        .eq(SysNotification::getBizType, bizType)
                        .eq(SysNotification::getBizId, bizId)
                        .ge(SysNotification::getCreateTime, threshold)
                        .orderByDesc(SysNotification::getCreateTime)
                        .last("LIMIT 1"));

        if (existing != null) {
            // 合并：更新原通知的标题/内容/时间戳，并重置为未读
            existing.setTitle(title);
            existing.setContent(content);
            existing.setCreateTime(now);
            existing.setIsRead(0);

            // E1: 合并时同步更新 priority 和 payload
            String receiverRoleCode = null;
            if (options == null || options.getActionUrl() == null) {
                SysUser receiver = userMapper.selectById(userId);
                if (receiver != null) {
                    receiverRoleCode = mapRoleIdToCode(receiver.getRoleId());
                }
            }
            applyOptions(existing, options, receiverRoleCode);

            notificationMapper.updateById(existing);

            final SysNotification pushSnapshot = existing;
            // 事务提交后推送，避免推送早于 update 提交的竞态
            pushAfterCommitOrNow("notifyUserWithDedupe.merge type=" + type + " userId=" + userId, () -> {
                try {
                    Map<String, Object> pushData = buildPushData(pushSnapshot);
                    pushService.pushToUser(userId, pushData);
                } catch (Exception e) {
                    log.error("推送合并通知失败，用户ID: {}, 类型: {}", userId, type, e);
                }
            });
        } else {
            // 窗口外或不存在：委托给扩展版 notifyUser
            notifyUser(userId, type, title, content, bizType, bizId, options);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void notifyClassStudents(Long classId, String type, String title, String content, String bizType, Long bizId) {
        notifyClassStudents(classId, type, title, content, bizType, bizId, null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void notifyClassStudents(Long classId, String type, String title, String content, String bizType, Long bizId,
                                    NotificationOptions options) {
        // 1. 查询班级所有学生
        List<SysUser> students = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getClassId, classId)
                .eq(SysUser::getRoleId, RoleConstants.STUDENT_ROLE_ID)
                .eq(SysUser::getStatus, 1));

        if (students.isEmpty()) {
            log.warn("班级 {} 没有学生，跳过通知发送", classId);
            return;
        }

        log.info("准备发送通知给班级 {} 的 {} 个学生，类型：{}", classId, students.size(), type);

        // 2. 批量构建通知对象 —— 班级学生全部是 STUDENT 角色，action_url 一次解析复用
        final String receiverRoleCode = RoleConstants.STUDENT_CODE;
        List<SysNotification> notifications = students.stream()
            .map(s -> {
                SysNotification n = new SysNotification();
                n.setUserId(s.getId());
                n.setType(type);
                n.setTitle(title);
                n.setContent(content);
                n.setBizType(bizType);
                n.setBizId(bizId);
                n.setIsRead(0);
                // E1: 注入 priority + payload（班级批量场景，action_url 按学生角色解析）
                applyOptions(n, options, receiverRoleCode);
                return n;
            })
            .collect(Collectors.toList());

        // 3. 批量插入数据库（MyBatis-Plus）
        boolean success = this.saveBatch(notifications, 500); // 每批500条
        if (!success) {
            log.error("批量插入通知失败，班级：{}", classId);
            throw new RuntimeException("批量插入通知失败");
        }

        log.info("✅ 成功发送通知给班级 {} 的 {} 个学生", classId, notifications.size());

        // 4. 事务提交后再异步推送 WebSocket
        // （关键：此方法带 @Transactional，saveBatch 后事务未提交，
        //  必须等事务提交后才能推送，避免「前端收到推送但 list 查不到」的竞态）
        final NotificationOptions finalOpts = options;
        pushAfterCommitOrNow("notifyClassStudents classId=" + classId + " type=" + type, () -> {
            try {
                Map<String, Object> pushData = new LinkedHashMap<>();
                pushData.put("type", type);
                pushData.put("title", title);
                pushData.put("content", content);
                pushData.put("bizType", bizType);
                pushData.put("bizId", bizId);
                pushData.put("createTime", LocalDateTime.now());
                // E1: 推送时也带上优先级和 payload（前端即可看到发送者/actionUrl）
                if (finalOpts != null) {
                    pushData.put("priority", finalOpts.getPriority());
                    if (finalOpts.hasPayloadData()) {
                        pushData.put("payload", finalOpts.buildPayloadMap());
                    }
                }

                pushService.pushToClass(classId, pushData);
            } catch (Exception e) {
                log.error("推送通知失败，班级ID: {}", classId, e);
            }
        });
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void notifyAdmins(String type, String title, String content, String bizType, Long bizId) {
        notifyAdmins(type, title, content, bizType, bizId, null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void notifyAdmins(String type, String title, String content, String bizType, Long bizId,
                             NotificationOptions options) {
        // 1. 查询所有管理员
        List<SysUser> admins = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRoleId, RoleConstants.ADMIN_ROLE_ID)
                .eq(SysUser::getStatus, 1));

        if (admins.isEmpty()) {
            log.warn("没有管理员，跳过通知发送");
            return;
        }

        log.info("准备发送通知给 {} 个管理员，类型：{}", admins.size(), type);

        // 2. 批量构建通知对象 —— 接收者全是 ADMIN 角色，action_url 一次解析复用
        final String receiverRoleCode = RoleConstants.ADMIN_CODE;
        List<SysNotification> notifications = admins.stream()
            .map(a -> {
                SysNotification n = new SysNotification();
                n.setUserId(a.getId());
                n.setType(type);
                n.setTitle(title);
                n.setContent(content);
                n.setBizType(bizType);
                n.setBizId(bizId);
                n.setIsRead(0);
                // E1: 注入 priority + payload
                applyOptions(n, options, receiverRoleCode);
                return n;
            })
            .collect(Collectors.toList());

        // 3. 批量插入
        boolean success = this.saveBatch(notifications, 500);
        if (!success) {
            log.error("批量插入通知失败（管理员）");
            throw new RuntimeException("批量插入通知失败");
        }

        log.info("✅ 成功发送通知给 {} 个管理员", notifications.size());

        // 4. 事务提交后再异步推送 WebSocket
        // （同 notifyClassStudents：此方法带 @Transactional，必须 afterCommit 后推送）
        final NotificationOptions finalOpts = options;
        pushAfterCommitOrNow("notifyAdmins type=" + type, () -> {
            try {
                Map<String, Object> pushData = new LinkedHashMap<>();
                pushData.put("type", type);
                pushData.put("title", title);
                pushData.put("content", content);
                pushData.put("bizType", bizType);
                pushData.put("bizId", bizId);
                pushData.put("createTime", LocalDateTime.now());
                // E1: 推送时也带上优先级和 payload
                if (finalOpts != null) {
                    pushData.put("priority", finalOpts.getPriority());
                    if (finalOpts.hasPayloadData()) {
                        pushData.put("payload", finalOpts.buildPayloadMap());
                    }
                }

                pushService.pushToAdmins(pushData);
            } catch (Exception e) {
                log.error("推送通知失败（管理员）", e);
            }
        });
    }

    // ===================== 查询通知方法 =====================

    @Override
    public PageResult<Map<String, Object>> listNotifications(Long userId, Integer page, Integer size,
                                                              String type, Integer isRead) {
        LambdaQueryWrapper<SysNotification> wrapper = new LambdaQueryWrapper<SysNotification>()
                .eq(SysNotification::getUserId, userId)
                .eq(type != null, SysNotification::getType, type)
                .eq(isRead != null, SysNotification::getIsRead, isRead)
                .orderByDesc(SysNotification::getCreateTime);

        Page<SysNotification> p = notificationMapper.selectPage(new Page<>(page, size), wrapper);

        List<Map<String, Object>> records = p.getRecords().stream().map(n -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", n.getId());
            m.put("type", n.getType());
            m.put("title", n.getTitle());
            m.put("content", n.getContent());
            m.put("bizType", n.getBizType());
            m.put("bizId", n.getBizId());
            m.put("isRead", n.getIsRead());
            m.put("createTime", n.getCreateTime());
            // E1: 同步返回 priority + payload，前端用于渲染优先级色标和发送者头像
            m.put("priority", n.getPriority() != null ? n.getPriority() : NotificationOptions.PRIORITY_NORMAL);
            Map<String, Object> payloadMap = deserializePayload(n.getPayload());
            if (payloadMap != null) {
                m.put("payload", payloadMap);
            }
            return m;
        }).collect(Collectors.toList());

        return new PageResult<>(records, p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Override
    public Map<String, Object> getUnreadCount(Long userId) {
        long total = notificationMapper.selectCount(new LambdaQueryWrapper<SysNotification>()
                .eq(SysNotification::getUserId, userId)
                .eq(SysNotification::getIsRead, 0));
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        return result;
    }

    @Override
    public void markAsRead(Long notificationId, Long userId) {
        notificationMapper.update(null, new LambdaUpdateWrapper<SysNotification>()
                .eq(SysNotification::getId, notificationId)
                .eq(SysNotification::getUserId, userId)
                .set(SysNotification::getIsRead, 1));
    }

    @Override
    public void markAllAsRead(Long userId) {
        notificationMapper.update(null, new LambdaUpdateWrapper<SysNotification>()
                .eq(SysNotification::getUserId, userId)
                .eq(SysNotification::getIsRead, 0)
                .set(SysNotification::getIsRead, 1));
    }

    @Override
    public int deleteNotification(Long notificationId, Long userId) {
        // 权限最小化：WHERE id = ? AND user_id = ? 避免越权删除他人通知
        return notificationMapper.delete(new LambdaQueryWrapper<SysNotification>()
                .eq(SysNotification::getId, notificationId)
                .eq(SysNotification::getUserId, userId));
    }

    @Override
    public int batchDeleteNotifications(List<Long> notificationIds, Long userId) {
        if (notificationIds == null || notificationIds.isEmpty()) return 0;
        // 权限最小化：WHERE id IN (...) AND user_id = ? 避免越权
        return notificationMapper.delete(new LambdaQueryWrapper<SysNotification>()
                .in(SysNotification::getId, notificationIds)
                .eq(SysNotification::getUserId, userId));
    }

    @Override
    public int batchMarkAsRead(List<Long> notificationIds, Long userId) {
        if (notificationIds == null || notificationIds.isEmpty()) return 0;
        return notificationMapper.update(null, new LambdaUpdateWrapper<SysNotification>()
                .in(SysNotification::getId, notificationIds)
                .eq(SysNotification::getUserId, userId)
                .eq(SysNotification::getIsRead, 0)
                .set(SysNotification::getIsRead, 1));
    }

    // ===================== 待办事项（实时聚合 - 性能优化版） =====================

    @Override
    public List<Map<String, Object>> getPendingItems(Long userId, Long roleId, Long classId) {
        List<Map<String, Object>> items = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        if (RoleConstants.STUDENT_ROLE_ID.equals(roleId) && classId != null) {
            // 学生待办：本班级未参加且未结束的考试
            // 关键：与 StudentExamServiceImpl.getMyExams 的可见性规则保持一致 ——
            //   仅展示「考试 startTime >= 学生入班/创建时间」之后的考试，
            //   避免新生看到入班前的旧考试导致「待办显示但考试列表查不到」的不一致体验。
            SysUser student = userMapper.selectById(userId);
            LambdaQueryWrapper<ExamExam> examWrapper = new LambdaQueryWrapper<ExamExam>()
                    .eq(ExamExam::getClassId, classId)
                    .gt(ExamExam::getEndTime, now);
            if (student != null && student.getCreateTime() != null) {
                examWrapper.ge(ExamExam::getStartTime, student.getCreateTime());
            }
            List<ExamExam> exams = examMapper.selectList(examWrapper);

            if (!exams.isEmpty()) {
                // 性能优化：一次性查询所有考试记录（避免 N+1）
                List<Long> examIds = exams.stream().map(ExamExam::getId).collect(Collectors.toList());
                List<Long> recordedExamIds = recordMapper.selectList(
                    new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getUserId, userId)
                        .in(ExamRecord::getExamId, examIds)
                ).stream().map(ExamRecord::getExamId).collect(Collectors.toList());

                // 过滤出未参加的考试
                exams.stream()
                    .filter(e -> !recordedExamIds.contains(e.getId()))
                    .forEach(exam -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("type", "EXAM_PENDING");
                        item.put("title", "待参加考试：" + exam.getExamName());
                        item.put("content", "考试时间：" + exam.getStartTime().format(FMT) + " ~ " + exam.getEndTime().format(FMT));
                        item.put("bizType", "exam");
                        item.put("bizId", exam.getId());
                        items.add(item);
                    });
            }

            // 学生待办：进行中但未交卷的考试
            List<ExamRecord> inProgress = recordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                    .eq(ExamRecord::getUserId, userId)
                    .eq(ExamRecord::getStatus, 1));

            if (!inProgress.isEmpty()) {
                List<Long> inProgressExamIds = inProgress.stream().map(ExamRecord::getExamId).collect(Collectors.toList());
                List<ExamExam> inProgressExams = examMapper.selectBatchIds(inProgressExamIds);

                inProgressExams.stream()
                    .filter(e -> now.isBefore(e.getEndTime()))
                    .forEach(exam -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("type", "EXAM_IN_PROGRESS");
                        item.put("title", "进行中：" + exam.getExamName());
                        item.put("content", "请在 " + exam.getEndTime().format(FMT) + " 前提交");
                        item.put("bizType", "exam");
                        item.put("bizId", exam.getId());
                        items.add(item);
                    });
            }

        } else if (RoleConstants.TEACHER_ROLE_ID.equals(roleId)) {
            // 教师待办：有待批改试卷的考试（性能优化版）
            List<ExamExam> myExams = examMapper.selectList(new LambdaQueryWrapper<ExamExam>()
                    .eq(ExamExam::getCreatorId, userId));

            if (!myExams.isEmpty()) {
                List<Long> examIds = myExams.stream().map(ExamExam::getId).collect(Collectors.toList());

                // 性能优化：使用 GROUP BY 一次性统计所有考试的待批改数量
                Map<Long, Long> countMap = new HashMap<>();
                List<ExamRecord> pendingRecords = recordMapper.selectList(
                    new LambdaQueryWrapper<ExamRecord>()
                        .select(ExamRecord::getExamId)
                        .in(ExamRecord::getExamId, examIds)
                        .eq(ExamRecord::getStatus, 2)
                );

                // 手动统计每个考试的待批改数量
                for (ExamRecord record : pendingRecords) {
                    countMap.put(record.getExamId(), countMap.getOrDefault(record.getExamId(), 0L) + 1);
                }

                myExams.forEach(exam -> {
                    Long count = countMap.getOrDefault(exam.getId(), 0L);
                    if (count > 0) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("type", "NEED_MARKING");
                        item.put("title", "待批改：" + exam.getExamName());
                        item.put("content", count + " 份试卷待批改");
                        item.put("bizType", "exam");
                        item.put("bizId", exam.getId());
                        items.add(item);
                    }
                });
            }
        }
        // 管理员暂无特定待办
        return items;
    }

    // ===================== 辅助方法 =====================

    /**
     * 在事务提交后异步推送 WebSocket，避免推送早于通知数据库事务提交的竞态。
     *
     * <p><b>语义</b>：
     * <ul>
     *   <li>当前在事务上下文中：注册 {@link TransactionSynchronization#afterCommit()} 回调，
     *       在事务提交后再启动异步推送。</li>
     *   <li>当前无事务上下文：直接启动异步推送（保持向后兼容）。</li>
     * </ul>
     *
     * <p><b>异常处理</b>：推送任务内部异常会被双层 try/catch 兜底，
     * 不会冒泡影响主调用链；调用方传入的 task 也建议自行 try/catch 以记录具体场景日志。
     *
     * <p><b>设计依据</b>：参见 Spring 官方 TransactionSynchronizationManager#registerSynchronization
     * 文档；项目内 MarkingServiceImpl/StudentExamServiceImpl/ExamServiceImpl/UserServiceImpl
     * 已采用同模式，本工具方法为通知服务自身收口。
     *
     * @param scene 场景描述（用于兜底日志定位）
     * @param pushTask 推送任务
     */
    private void pushAfterCommitOrNow(String scene, Runnable pushTask) {
        Runnable safeAsync = () -> CompletableFuture.runAsync(() -> {
            try {
                pushTask.run();
            } catch (Exception e) {
                // 兜底捕获：调用方 task 内部通常已有 try/catch，这里防御性兜底
                log.error("WebSocket 推送任务执行失败（兜底），场景: {}", scene, e);
            }
        });

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    safeAsync.run();
                }
            });
        } else {
            safeAsync.run();
        }
    }

    private Map<String, Object> buildPushData(SysNotification n) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", n.getId());
        data.put("type", n.getType());
        data.put("title", n.getTitle());
        data.put("content", n.getContent());
        data.put("bizType", n.getBizType());
        data.put("bizId", n.getBizId());
        data.put("createTime", n.getCreateTime());
        // E1: 推送时同步携带优先级 + payload（反序列化为 Map 让前端直接使用）
        data.put("priority", n.getPriority() != null ? n.getPriority() : NotificationOptions.PRIORITY_NORMAL);
        Map<String, Object> payloadMap = deserializePayload(n.getPayload());
        if (payloadMap != null) {
            data.put("payload", payloadMap);
        }
        return data;
    }
}
