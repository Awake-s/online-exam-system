package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.common.constants.RoleConstants;
import com.exam.common.exception.BusinessException;
import com.exam.common.result.PageResult;
import com.exam.dto.request.ExamPublishRequest;
import com.exam.entity.*;
import com.exam.mapper.*;
import com.exam.service.ExamService;
import com.exam.service.NotificationOptions;
import com.exam.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ExamServiceImpl implements ExamService {

    @Autowired private ExamExamMapper examMapper;
    @Autowired private ExamPaperMapper paperMapper;
    @Autowired private EduClassMapper classMapper;
    @Autowired private SysUserMapper userMapper;
    @Autowired private ExamRecordMapper recordMapper;
    @Autowired private NotificationService notificationService;
    // 发布考试时「试卷科目 vs 班级专业」兑底校验需要：
    //   subjectMajorMapper 按 paper.subjectId 查出允许专业集合；
    //   subjectMapper 拼接错误消息中的科目名称。
    @Autowired private SubjectMajorMapper subjectMajorMapper;
    @Autowired private EduSubjectMapper subjectMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public PageResult<Map<String, Object>> listExams(Integer page, Integer size, String examName, Integer status, Long classId, Long subjectId, Long creatorId) {
        LambdaQueryWrapper<ExamExam> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamExam::getCreatorId, creatorId);
        if (StringUtils.hasText(examName)) wrapper.like(ExamExam::getExamName, examName);
        if (classId != null) wrapper.eq(ExamExam::getClassId, classId);
        // 科目筛选：试卷表才有 subjectId，通过试卷ID子查询桥接
        if (subjectId != null) {
            List<Long> subjectPaperIds = paperMapper.selectList(
                    new LambdaQueryWrapper<ExamPaper>().eq(ExamPaper::getSubjectId, subjectId)
            ).stream().map(ExamPaper::getId).collect(Collectors.toList());
            if (subjectPaperIds.isEmpty()) {
                return new PageResult<>(Collections.emptyList(), 0L, page.longValue(), size.longValue());
            }
            wrapper.in(ExamExam::getPaperId, subjectPaperIds);
        }
        LocalDateTime now = LocalDateTime.now();
        if (status != null) {
            if (status == 0) wrapper.gt(ExamExam::getStartTime, now);
            else if (status == 1) wrapper.le(ExamExam::getStartTime, now).ge(ExamExam::getEndTime, now);
            else if (status == 2) wrapper.lt(ExamExam::getEndTime, now);
        }
        wrapper.orderByDesc(ExamExam::getCreateTime);

        Page<ExamExam> p = examMapper.selectPage(new Page<>(page, size), wrapper);

        List<Long> paperIds = p.getRecords().stream().map(ExamExam::getPaperId).distinct().collect(Collectors.toList());
        List<Long> classIds = p.getRecords().stream().map(ExamExam::getClassId).distinct().collect(Collectors.toList());

        Map<Long, String> paperMap = paperIds.isEmpty() ? Collections.emptyMap() :
                paperMapper.selectBatchIds(paperIds).stream()
                        .collect(Collectors.toMap(ExamPaper::getId, ExamPaper::getPaperName));
        Map<Long, String> classMap = classIds.isEmpty() ? Collections.emptyMap() :
                classMapper.selectBatchIds(classIds).stream()
                        .collect(Collectors.toMap(EduClass::getId, EduClass::getClassName));

        List<Map<String, Object>> records = p.getRecords().stream().map(e -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", e.getId());
            m.put("examName", e.getExamName());
            m.put("paperId", e.getPaperId());
            m.put("paperName", paperMap.getOrDefault(e.getPaperId(), ""));
            m.put("classId", e.getClassId());
            m.put("className", classMap.getOrDefault(e.getClassId(), ""));
            m.put("startTime", e.getStartTime());
            m.put("endTime", e.getEndTime());

            int computedStatus;
            if (now.isBefore(e.getStartTime())) computedStatus = 0;
            else if (now.isAfter(e.getEndTime())) computedStatus = 2;
            else computedStatus = 1;
            m.put("status", computedStatus);
            String[] statusNames = {"未开始", "进行中", "已结束"};
            m.put("statusName", statusNames[computedStatus]);

            long totalStudents = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getClassId, e.getClassId()).eq(SysUser::getRoleId, RoleConstants.STUDENT_ROLE_ID));
            m.put("totalStudents", totalStudents);
            long submittedCount = recordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>()
                    .eq(ExamRecord::getExamId, e.getId()).in(ExamRecord::getStatus, 2, 3));
            m.put("submittedCount", submittedCount);
            long absentCount = recordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>()
                    .eq(ExamRecord::getExamId, e.getId()).eq(ExamRecord::getStatus, 4));
            m.put("absentCount", absentCount);
            m.put("scorePublished", Integer.valueOf(1).equals(e.getScorePublished()));
            // 防作弊配置
            if (e.getAntiCheatConfig() != null) {
                try { m.put("antiCheat", objectMapper.readValue(e.getAntiCheatConfig(), Map.class)); } catch (Exception ignored) { m.put("antiCheat", null); }
            } else {
                m.put("antiCheat", null);
            }
            m.put("createTime", e.getCreateTime());
            return m;
        }).collect(Collectors.toList());

        return new PageResult<>(records, p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Override
    @Transactional
    public void publishExam(ExamPublishRequest request, Long creatorId) {
        ExamPaper paper = paperMapper.selectById(request.getPaperId());
        if (paper == null) throw new BusinessException("试卷不存在");
        if (!paper.getCreatorId().equals(creatorId)) throw new BusinessException("只能使用自己创建的试卷");
        if (paper.getStatus() != 1) throw new BusinessException("试卷尚未发布，请先发布试卷再创建考试");

        if (request.getStartTime().isBefore(LocalDateTime.now())) throw new BusinessException("开始时间必须晚于当前时间");
        if (request.getEndTime().isBefore(request.getStartTime())) throw new BusinessException("结束时间必须晚于开始时间");

        List<Long> classIds = request.getClassIds();
        if (classIds == null || classIds.isEmpty()) {
            if (request.getClassId() == null) throw new BusinessException("请选择班级");
            classIds = Collections.singletonList(request.getClassId());
        }

        // 兑底校验：试卷绑定的科目所属专业集合必须包含所有目标班级的专业。
        // 防止前端 UI 联动过滤被绕过（如 Postman / 脚本直接调 API）导致跨专业误发考试。
        // 对齐：OWASP 「Defense in Depth」 + 国内教务系统（超星泛雅 / 正方）「作业发布范围强校验」设计。
        // 数据链路：paper.subjectId → subject_major(多对多) → 与 class.major_id 比对。
        List<SubjectMajor> subjectMajors = subjectMajorMapper.selectList(
                new LambdaQueryWrapper<SubjectMajor>().eq(SubjectMajor::getSubjectId, paper.getSubjectId()));
        Set<Long> allowedMajorIds = subjectMajors.stream()
                .map(SubjectMajor::getMajorId).collect(Collectors.toSet());
        // 仅当科目有专业关联时才校验；空关联（旧迁移期数据 / 公共课未完成关联配置）不阻断。
        if (!allowedMajorIds.isEmpty()) {
            List<String> mismatchedClasses = new ArrayList<>();
            for (Long classId : classIds) {
                EduClass cls = classMapper.selectById(classId);
                // 班级无 majorId（旧数据未迁移）→ 跳过该班级校验，绝不错杀
                if (cls != null && cls.getMajorId() != null && !allowedMajorIds.contains(cls.getMajorId())) {
                    mismatchedClasses.add(cls.getClassName());
                }
            }
            if (!mismatchedClasses.isEmpty()) {
                EduSubject subj = subjectMapper.selectById(paper.getSubjectId());
                String subjName = subj != null ? subj.getSubjectName() : "该试卷";
                throw new BusinessException("以下班级所属专业未开设「" + subjName + "」课程，无法发布考试：" + String.join("、", mismatchedClasses));
            }
        }

        // 先检查所有班级的冲突情况
        List<String> conflictClasses = new ArrayList<>();
        for (Long classId : classIds) {
            long overlap = examMapper.selectCount(new LambdaQueryWrapper<ExamExam>()
                    .eq(ExamExam::getClassId, classId)
                    .lt(ExamExam::getStartTime, request.getEndTime())
                    .gt(ExamExam::getEndTime, request.getStartTime()));
            if (overlap > 0) {
                EduClass eduClass = classMapper.selectById(classId);
                conflictClasses.add(eduClass != null ? eduClass.getClassName() : String.valueOf(classId));
            }
        }
        if (!conflictClasses.isEmpty()) {
            throw new BusinessException("以下班级在此时间段已有考试安排：" + String.join("、", conflictClasses));
        }

        // 无冲突后统一插入，记录每个班级对应的 examId
        Map<Long, Long> classExamIdMap = new HashMap<>();
        for (Long classId : classIds) {
            ExamExam exam = new ExamExam();
            exam.setExamName(request.getExamName());
            exam.setPaperId(request.getPaperId());
            exam.setClassId(classId);
            exam.setStartTime(request.getStartTime());
            exam.setEndTime(request.getEndTime());
            exam.setCreatorId(creatorId);
            exam.setStatus(0);
            if (request.getAntiCheat() != null) {
                try { exam.setAntiCheatConfig(objectMapper.writeValueAsString(request.getAntiCheat())); } catch (Exception ignored) {}
            }
            examMapper.insert(exam);
            classExamIdMap.put(classId, exam.getId());
        }

        // 通知与 WebSocket 推送延迟到事务提交后执行，避免事务回滚后通知已外发导致数据不一致。
        // 对齐 MarkingServiceImpl.publishScores / StudentExamServiceImpl 已有的 afterCommit 模式。
        SysUser teacher = userMapper.selectById(creatorId);
        final String teacherName = teacher != null ? teacher.getRealName() : "教师";
        final String teacherAvatar = teacher != null ? teacher.getAvatar() : null;
        final String examName = request.getExamName();
        final String timeRange = request.getStartTime().toLocalDate() + " " + request.getStartTime().toLocalTime().withSecond(0)
                + " ~ " + request.getEndTime().toLocalTime().withSecond(0);
        final List<Long> finalClassIds = new ArrayList<>(classIds);
        final Map<Long, Long> finalClassExamIdMap = new HashMap<>(classExamIdMap);
        final Long finalCreatorId = creatorId;

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // 事务已提交，通知失败不可使主业务回滚；仅记录日志
                    try {
                        doSendPublishExamNotifications(finalCreatorId, teacherName, teacherAvatar,
                                examName, timeRange, finalClassIds, finalClassExamIdMap);
                    } catch (Exception e) {
                        log.error("发布考试后发送通知失败 examName={} classIds={}", examName, finalClassIds, e);
                    }
                }
            });
        } else {
            try {
                doSendPublishExamNotifications(creatorId, teacherName, teacherAvatar,
                        examName, timeRange, classIds, classExamIdMap);
            } catch (Exception e) {
                log.error("发布考试后发送通知失败（无事务上下文）examName={}", examName, e);
            }
        }
    }

    /**
     * 发布考试后的通知推送。
     * <p>提取为独立方法以便在 afterCommit 中复用，避免事务提交前误发。
     */
    private void doSendPublishExamNotifications(Long creatorId, String teacherName, String teacherAvatar,
                                                  String examName, String timeRange,
                                                  List<Long> classIds, Map<Long, Long> classExamIdMap) {
        for (Long classId : classIds) {
            notificationService.notifyClassStudents(classId, "EXAM_PUBLISHED",
                    "新考试：" + examName,
                    "考试时间：" + timeRange + "，请按时参加",
                    "exam", classExamIdMap.get(classId),
                    NotificationOptions.defaults().withSender(creatorId, teacherName, teacherAvatar));
        }
        Long firstExamId = classExamIdMap.values().stream().findFirst().orElse(null);
        notificationService.notifyAdmins("EXAM_CREATED",
                teacherName + " 发布了新考试",
                "考试名称：" + examName,
                "exam", firstExamId,
                NotificationOptions.defaults().withSender(creatorId, teacherName, teacherAvatar));
    }

    @Override
    @Transactional
    public void updateExam(Long id, ExamPublishRequest request, Long creatorId) {
        ExamExam exam = examMapper.selectById(id);
        if (exam == null) throw new BusinessException("考试不存在");
        if (!exam.getCreatorId().equals(creatorId)) throw new BusinessException("只能编辑自己发布的考试");

        LocalDateTime now = LocalDateTime.now();
        if (!now.isBefore(exam.getStartTime())) throw new BusinessException("只能编辑未开始的考试");

        if (request.getStartTime().isBefore(now)) throw new BusinessException("开始时间必须晚于当前时间");
        if (request.getEndTime().isBefore(request.getStartTime())) throw new BusinessException("结束时间必须晚于开始时间");

        Long targetClassId = request.getClassId();
        if (targetClassId == null && request.getClassIds() != null && !request.getClassIds().isEmpty()) {
            targetClassId = request.getClassIds().get(0);
        }
        if (targetClassId == null) targetClassId = exam.getClassId();

        long overlap = examMapper.selectCount(new LambdaQueryWrapper<ExamExam>()
                .eq(ExamExam::getClassId, targetClassId)
                .ne(ExamExam::getId, id)
                .lt(ExamExam::getStartTime, request.getEndTime())
                .gt(ExamExam::getEndTime, request.getStartTime()));
        if (overlap > 0) throw new BusinessException("该班级在此时间段已有考试安排");

        // 兑底校验：与 publishExam 对齐，防止编辑时被改成跨专业不匹配的「试卷+班级」组合。
        // 防止前端 UI 联动过滤被绕过（如 Postman / 脚本直接调 PUT /api/exam/update/{id}）。
        // 对齐：OWASP「Defense in Depth」+ publishExam 同款校验，保持发布/编辑两路径防御对称。
        // 数据链路：paper.subjectId → subject_major(多对多) → 与目标班级 class.major_id 比对。
        ExamPaper updatePaper = paperMapper.selectById(request.getPaperId());
        if (updatePaper != null && updatePaper.getSubjectId() != null) {
            List<SubjectMajor> updateSubjectMajors = subjectMajorMapper.selectList(
                    new LambdaQueryWrapper<SubjectMajor>().eq(SubjectMajor::getSubjectId, updatePaper.getSubjectId()));
            Set<Long> updateAllowedMajorIds = updateSubjectMajors.stream()
                    .map(SubjectMajor::getMajorId).collect(Collectors.toSet());
            // 仅当科目有专业关联时才校验；空关联（旧迁移期数据 / 公共课未完成关联配置）不阻断。
            if (!updateAllowedMajorIds.isEmpty()) {
                EduClass targetCls = classMapper.selectById(targetClassId);
                // 班级无 majorId（旧数据未迁移）→ 跳过校验，绝不错杀
                if (targetCls != null && targetCls.getMajorId() != null
                        && !updateAllowedMajorIds.contains(targetCls.getMajorId())) {
                    EduSubject updateSubj = subjectMapper.selectById(updatePaper.getSubjectId());
                    String updateSubjName = updateSubj != null ? updateSubj.getSubjectName() : "该试卷";
                    throw new BusinessException("班级「" + targetCls.getClassName() + "」所属专业未开设「"
                            + updateSubjName + "」课程，无法保存考试");
                }
            }
        }

        exam.setExamName(request.getExamName());
        exam.setPaperId(request.getPaperId());
        exam.setClassId(targetClassId);
        exam.setStartTime(request.getStartTime());
        exam.setEndTime(request.getEndTime());
        if (request.getAntiCheat() != null) {
            try { exam.setAntiCheatConfig(objectMapper.writeValueAsString(request.getAntiCheat())); } catch (Exception ignored) {}
        }
        examMapper.updateById(exam);

        // 通知延迟到事务提交后执行（避免事务回滚后考试变更通知已外发）
        SysUser updateTeacher = userMapper.selectById(creatorId);
        final Long finalUpdateExamId = id;
        final Long finalUpdateClassId = exam.getClassId();
        final String finalUpdateExamName = request.getExamName();
        final Long finalUpdateCreatorId = creatorId;
        final String finalUpdateTeacherName = updateTeacher != null ? updateTeacher.getRealName() : "教师";
        final String finalUpdateTeacherAvatar = updateTeacher != null ? updateTeacher.getAvatar() : null;

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        doSendUpdateExamNotification(finalUpdateClassId, finalUpdateExamId, finalUpdateExamName,
                                finalUpdateCreatorId, finalUpdateTeacherName, finalUpdateTeacherAvatar);
                    } catch (Exception e) {
                        log.error("更新考试后发送通知失败 examId={} examName={}", finalUpdateExamId, finalUpdateExamName, e);
                    }
                }
            });
        } else {
            try {
                doSendUpdateExamNotification(exam.getClassId(), id, request.getExamName(), creatorId,
                        updateTeacher != null ? updateTeacher.getRealName() : "教师",
                        updateTeacher != null ? updateTeacher.getAvatar() : null);
            } catch (Exception e) {
                log.error("更新考试后发送通知失败（无事务上下文）examId={}", id, e);
            }
        }
    }

    /** 考试变更通知（仅供 updateExam 调用，避免在事务未提交前推送） */
    private void doSendUpdateExamNotification(Long classId, Long examId, String examName,
                                                Long creatorId, String teacherName, String teacherAvatar) {
        notificationService.notifyClassStudents(classId, "EXAM_UPDATED",
                "考试变更：" + examName,
                "考试时间已更新，请注意查看",
                "exam", examId,
                NotificationOptions.priority(NotificationOptions.PRIORITY_URGENT)
                        .withSender(creatorId, teacherName, teacherAvatar));
    }

    @Override
    @Transactional
    public void deleteExam(Long id, Long creatorId) {
        ExamExam exam = examMapper.selectById(id);
        if (exam == null) throw new BusinessException("考试不存在");
        if (!exam.getCreatorId().equals(creatorId)) throw new BusinessException("只能删除自己发布的考试");

        LocalDateTime now = LocalDateTime.now();
        if (!now.isBefore(exam.getStartTime())) throw new BusinessException("只能删除未开始的考试");

        long recordCount = recordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>().eq(ExamRecord::getExamId, id));
        if (recordCount > 0) throw new BusinessException("已有学生参加考试，无法删除");

        // 提前提取通知所需信息（考试名称/班级/教师信息），
        // 之后执行删除，最后在事务提交后发送通知。
        // 这样保证：只有在考试真正被删除（事务提交）后，学生才会收到取消通知。
        SysUser cancelTeacher = userMapper.selectById(creatorId);
        final Long finalCancelClassId = exam.getClassId();
        final String finalCancelExamName = exam.getExamName();
        final Long finalCancelCreatorId = creatorId;
        final String finalCancelTeacherName = cancelTeacher != null ? cancelTeacher.getRealName() : "教师";
        final String finalCancelTeacherAvatar = cancelTeacher != null ? cancelTeacher.getAvatar() : null;

        examMapper.deleteById(id);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        doSendCancelExamNotification(finalCancelClassId, finalCancelExamName,
                                finalCancelCreatorId, finalCancelTeacherName, finalCancelTeacherAvatar);
                    } catch (Exception e) {
                        log.error("取消考试后发送通知失败 examName={} classId={}", finalCancelExamName, finalCancelClassId, e);
                    }
                }
            });
        } else {
            try {
                doSendCancelExamNotification(exam.getClassId(), exam.getExamName(), creatorId,
                        cancelTeacher != null ? cancelTeacher.getRealName() : "教师",
                        cancelTeacher != null ? cancelTeacher.getAvatar() : null);
            } catch (Exception e) {
                log.error("取消考试后发送通知失败（无事务上下文）examId={}", id, e);
            }
        }
    }

    /** 考试取消通知（仅供 deleteExam 调用，避免删除事务未提交即外发） */
    private void doSendCancelExamNotification(Long classId, String examName, Long creatorId,
                                                String teacherName, String teacherAvatar) {
        notificationService.notifyClassStudents(classId, "EXAM_CANCELLED",
                "考试取消：" + examName,
                "该考试已被取消",
                "exam", null,
                NotificationOptions.priority(NotificationOptions.PRIORITY_URGENT)
                        .withSender(creatorId, teacherName, teacherAvatar));
    }

    @Override
    public List<Map<String, Object>> getExamRecords(Long examId, Long creatorId) {
        ExamExam exam = examMapper.selectById(examId);
        if (exam == null) throw new BusinessException("考试不存在");
        if (!exam.getCreatorId().equals(creatorId)) throw new BusinessException("无权查看");

        // 纯查询接口，不在此进行写入：
        // 缺考记录由 ExamEndTask 定时任务（每 60 秒一次）统一生成，
        // 避免多会话并发触发写入时出现唯一约束冲突与数据不一致。

        List<ExamRecord> records = recordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, examId).orderByDesc(ExamRecord::getTotalScore));

        // 获取班级所有学生，用于补充未进入考试的学生
        List<SysUser> allStudents = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getClassId, exam.getClassId())
                .eq(SysUser::getRoleId, RoleConstants.STUDENT_ROLE_ID));
        Map<Long, SysUser> allStudentMap = allStudents.stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));

        // 已有记录的学生ID集合
        java.util.Set<Long> recordedUserIds = records.stream()
                .map(ExamRecord::getUserId).collect(Collectors.toSet());

        Map<Long, String> classMap = classMapper.selectList(null).stream()
                .collect(Collectors.toMap(EduClass::getId, EduClass::getClassName));
        String[] statusNames = {"未开始", "答题中", "已交卷", "已批改", "缺考"};

        List<Map<String, Object>> result = records.stream().map(r -> {
            SysUser user = allStudentMap.get(r.getUserId());
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("userId", r.getUserId());
            m.put("realName", user != null ? user.getRealName() : "");
            m.put("className", user != null && user.getClassId() != null ? classMap.get(user.getClassId()) : "");
            m.put("startTime", r.getStartTime());
            m.put("submitTime", r.getSubmitTime());
            m.put("totalScore", r.getTotalScore());
            m.put("objectiveScore", r.getObjectiveScore());
            m.put("subjectiveScore", r.getSubjectiveScore());
            m.put("status", r.getStatus());
            m.put("statusName", r.getStatus() != null && r.getStatus() >= 0 && r.getStatus() <= 4 ? statusNames[r.getStatus()] : "");
            m.put("switchCount", r.getSwitchCount() != null ? r.getSwitchCount() : 0);
            return m;
        }).collect(Collectors.toCollection(ArrayList::new));

        // 补充未进入考试的学生（虚拟状态 -1 = "未进入"）
        for (SysUser student : allStudents) {
            if (!recordedUserIds.contains(student.getId())) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", null);
                m.put("userId", student.getId());
                m.put("realName", student.getRealName());
                m.put("className", student.getClassId() != null ? classMap.get(student.getClassId()) : "");
                m.put("startTime", null);
                m.put("submitTime", null);
                m.put("totalScore", null);
                m.put("objectiveScore", null);
                m.put("subjectiveScore", null);
                m.put("status", -1);
                m.put("statusName", "未进入");
                m.put("switchCount", 0);
                result.add(m);
            }
        }

        return result;
    }

}
