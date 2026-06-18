package com.exam.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.common.constants.RoleConstants;
import com.exam.entity.*;
import com.exam.mapper.*;
import com.exam.service.NotificationOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Component
public class ExamEndTask {

    private static final Logger log = LoggerFactory.getLogger(ExamEndTask.class);

    @Autowired private ExamExamMapper examMapper;
    @Autowired private ExamRecordMapper recordMapper;
    @Autowired private ExamAnswerMapper answerMapper;
    @Autowired private ExamPaperQuestionMapper paperQuestionMapper;
    @Autowired private ExamQuestionMapper questionMapper;
    @Autowired private ExamPaperMapper paperMapper;
    @Autowired private SysUserMapper userMapper;
    @Autowired private com.exam.service.NotificationService notificationService;
    @Autowired private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    @Autowired private TransactionTemplate transactionTemplate;

    @Scheduled(fixedDelay = 60000)
    public void processEndedExams() {
        LocalDateTime now = LocalDateTime.now();

        // 只处理刚结束不久的考试（2小时内），避免对已处理完毕的考试反复查询
        List<ExamExam> endedExams = examMapper.selectList(new LambdaQueryWrapper<ExamExam>()
                .lt(ExamExam::getEndTime, now)
                .gt(ExamExam::getEndTime, now.minusHours(2)));

        for (ExamExam exam : endedExams) {
            try {
                processOneExam(exam);
            } catch (Exception e) {
                log.error("处理考试[{}]结束任务失败: {}", exam.getId(), e.getMessage());
            }
        }
    }

    private void processOneExam(ExamExam exam) {
        List<ExamRecord> unsubmitted = recordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, exam.getId())
                .eq(ExamRecord::getStatus, 1));

        int autoSubmittedCount = 0;
        for (ExamRecord record : unsubmitted) {
            boolean submitted = autoSubmitRecord(record);
            if (!submitted) {
                // CAS 失败：已被学生手动交卷抢先，跳过此记录不重复发通知
                continue;
            }
            autoSubmittedCount++;
            log.info("考试[{}] 学生[{}] 自动提交", exam.getId(), record.getUserId());

            // 交卷事务已提交，在事务外发送通知与 WebSocket，避免回滚后通知外发
            try {
                // E1: 系统自动事件，priority=3 次要（考后知晓，无需紧急）
                notificationService.notifyUser(record.getUserId(), "EXAM_AUTO_SUBMITTED",
                        "考试已自动提交",
                        "考试「" + exam.getExamName() + "」已超时，系统已自动提交您的答卷",
                        "exam", exam.getId(),
                        NotificationOptions.priority(NotificationOptions.PRIORITY_LOW));
                Map<String, Object> wsEvent = new HashMap<>();
                wsEvent.put("type", "EXAM_AUTO_SUBMITTED");
                wsEvent.put("examId", exam.getId());
                wsEvent.put("examName", exam.getExamName());
                messagingTemplate.convertAndSendToUser(
                        String.valueOf(record.getUserId()),
                        "/queue/exam-events", wsEvent);
            } catch (Exception e) {
                log.warn("自动交卷通知发送失败: {}", e.getMessage());
            }
        }

        List<SysUser> students = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getClassId, exam.getClassId())
                .eq(SysUser::getRoleId, RoleConstants.STUDENT_ROLE_ID)
                .eq(SysUser::getStatus, 1)
                .le(SysUser::getCreateTime, exam.getStartTime()));

        List<ExamRecord> existingRecords = recordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, exam.getId()));
        Set<Long> recordedUserIds = existingRecords.stream()
                .map(ExamRecord::getUserId).collect(Collectors.toSet());

        int absentCount = 0;
        for (SysUser student : students) {
            if (!recordedUserIds.contains(student.getId())) {
                // 每个缺考记录单独事务，避免一个失败影响其他学生和自动交卷结果
                Boolean inserted = transactionTemplate.execute(status -> {
                    ExamRecord absentRecord = new ExamRecord();
                    absentRecord.setExamId(exam.getId());
                    absentRecord.setUserId(student.getId());
                    absentRecord.setPaperId(exam.getPaperId());
                    absentRecord.setStartTime(null);
                    absentRecord.setSubmitTime(exam.getEndTime());
                    absentRecord.setTotalScore(BigDecimal.ZERO);
                    absentRecord.setObjectiveScore(BigDecimal.ZERO);
                    absentRecord.setSubjectiveScore(BigDecimal.ZERO);
                    absentRecord.setStatus(4);
                    try {
                        recordMapper.insert(absentRecord);
                        return true;
                    } catch (Exception e) {
                        status.setRollbackOnly();
                        log.debug("考试[{}] 学生[{}] 缺考记录已存在", exam.getId(), student.getId());
                        return false;
                    }
                });
                if (Boolean.TRUE.equals(inserted)) {
                    absentCount++;
                    log.info("考试[{}] 学生[{}] 标记缺考", exam.getId(), student.getId());

                    // 事务提交后在事务外发通知，避免与事务纠缠
                    try {
                        // E1: 系统自动事件，priority=3 次要
                        notificationService.notifyUser(student.getId(), "EXAM_ABSENT",
                                "缺考通知",
                                "您未参加考试「" + exam.getExamName() + "」，已被标记为缺考",
                                "exam", exam.getId(),
                                NotificationOptions.priority(NotificationOptions.PRIORITY_LOW));
                    } catch (Exception e) {
                        log.warn("缺考通知发送失败: {}", e.getMessage());
                    }
                }
            }
        }

        // 通知出卷教师：考试结束后有新的自动交卷/缺考记录需要处理
        int autoSubmitCount = autoSubmittedCount;
        if (autoSubmitCount > 0 || absentCount > 0) {
            try {
                StringBuilder content = new StringBuilder("考试「" + exam.getExamName() + "」已结束。");
                if (autoSubmitCount > 0) content.append("系统自动提交 ").append(autoSubmitCount).append(" 份试卷");
                if (autoSubmitCount > 0 && absentCount > 0) content.append("，");
                if (absentCount > 0) content.append(absentCount).append(" 名学生缺考");
                content.append("，请及时批改。");

                // E1: 考试结束汇总，priority=2 普通（默认）
                notificationService.notifyUser(exam.getCreatorId(), "EXAM_END_SUMMARY",
                        "考试结束：" + exam.getExamName(),
                        content.toString(),
                        "exam", exam.getId());

                // WebSocket 实时推送给教师，让阅卷管理页面即时刷新
                Map<String, Object> wsEvent = new HashMap<>();
                wsEvent.put("type", "EXAM_END_SUMMARY");
                wsEvent.put("examId", exam.getId());
                wsEvent.put("examName", exam.getExamName());
                wsEvent.put("autoSubmitCount", autoSubmitCount);
                wsEvent.put("absentCount", absentCount);
                messagingTemplate.convertAndSendToUser(
                        String.valueOf(exam.getCreatorId()),
                        "/queue/exam-events", wsEvent);
                log.info("考试[{}] 结束汇总已通知教师[{}]：自动提交{}份，缺考{}人",
                        exam.getId(), exam.getCreatorId(), autoSubmitCount, absentCount);
            } catch (Exception e) {
                log.warn("考试结束通知教师失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 单条自动交卷：在独立事务中完成评分与 CAS 更新。
     * 返回 true 表示 CAS 成功（record 从答题中 → 已提交/已批改）；
     * 返回 false 表示记录已被其它路径（学生手动交卷）处理，本次无需重复通知。
     */
    private boolean autoSubmitRecord(ExamRecord record) {
        final AtomicReference<Boolean> resultRef = new AtomicReference<>(Boolean.FALSE);
        transactionTemplate.execute(status -> {
            List<ExamAnswer> savedAnswers = answerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                    .eq(ExamAnswer::getRecordId, record.getId()));

            List<ExamPaperQuestion> pqs = paperQuestionMapper.selectList(
                    new LambdaQueryWrapper<ExamPaperQuestion>().eq(ExamPaperQuestion::getPaperId, record.getPaperId()));
            Map<Long, ExamPaperQuestion> pqMap = pqs.stream()
                    .collect(Collectors.toMap(ExamPaperQuestion::getQuestionId, pq -> pq));

            BigDecimal objectiveScore = BigDecimal.ZERO;
            boolean allBlank = savedAnswers.isEmpty() || savedAnswers.stream()
                    .allMatch(a -> a.getAnswer() == null || a.getAnswer().trim().isEmpty());

            // 从试卷题目推断是否包含主观题（而非从已保存答案推断，避免学生未答主观题时误判）
            boolean hasSubjective = false;
            for (ExamPaperQuestion pq : pqs) {
                ExamQuestion q = questionMapper.selectById(pq.getQuestionId());
                if (q != null && q.getQuestionType() == 5) {
                    hasSubjective = true;
                    break;
                }
            }

            for (ExamAnswer answer : savedAnswers) {
                ExamQuestion question = questionMapper.selectById(answer.getQuestionId());
                ExamPaperQuestion pq = pqMap.get(answer.getQuestionId());
                if (question == null || pq == null) continue;

                int type = question.getQuestionType();
                BigDecimal fullScore = pq.getScore();

                if (type == 5) {
                    // 主观题跳过自动评分
                } else {
                    BigDecimal scored = autoGrade(type, answer.getAnswer(), question.getAnswer(), fullScore);
                    answer.setScore(scored);
                    if (scored.compareTo(fullScore) == 0) answer.setIsCorrect(1);
                    else if (scored.compareTo(BigDecimal.ZERO) == 0) answer.setIsCorrect(0);
                    else answer.setIsCorrect(2);
                    answerMapper.updateById(answer);
                    objectiveScore = objectiveScore.add(scored);
                }
            }

            int newStatus;
            BigDecimal subjectiveForUpdate;
            BigDecimal totalForUpdate;
            if (!hasSubjective && !allBlank) {
                newStatus = 3;
                subjectiveForUpdate = BigDecimal.ZERO;
                totalForUpdate = objectiveScore;
            } else {
                newStatus = 2;
                subjectiveForUpdate = null;
                totalForUpdate = null;
            }
            int affected = recordMapper.casSubmitFromInProgress(record.getId(), newStatus, LocalDateTime.now(),
                    objectiveScore, subjectiveForUpdate, totalForUpdate);
            if (affected == 0) {
                // 并发场景：学生已手动交卷，回滚本次评分写入
                status.setRollbackOnly();
                resultRef.set(Boolean.FALSE);
            } else {
                resultRef.set(Boolean.TRUE);
            }
            return null;
        });
        return Boolean.TRUE.equals(resultRef.get());
    }

    private BigDecimal autoGrade(int type, String studentAnswer, String correctAnswer, BigDecimal fullScore) {
        if (studentAnswer == null || studentAnswer.trim().isEmpty()) return BigDecimal.ZERO;
        if (correctAnswer == null || correctAnswer.trim().isEmpty()) return BigDecimal.ZERO;

        switch (type) {
            case 1:
            case 3:
                return studentAnswer.trim().equalsIgnoreCase(correctAnswer.trim()) ? fullScore : BigDecimal.ZERO;
            case 2:
                Set<String> correctSet = Arrays.stream(correctAnswer.split(","))
                        .map(String::trim).map(String::toUpperCase).collect(Collectors.toSet());
                Set<String> studentSet = Arrays.stream(studentAnswer.split(","))
                        .map(String::trim).map(String::toUpperCase).collect(Collectors.toSet());
                if (studentSet.equals(correctSet)) return fullScore;
                if (correctSet.containsAll(studentSet) && !studentSet.isEmpty()) {
                    return fullScore.multiply(new BigDecimal("0.5")).setScale(2, RoundingMode.HALF_UP);
                }
                return BigDecimal.ZERO;
            case 4:
                String[] correctParts = correctAnswer.split(",");
                if (correctParts.length == 0) return BigDecimal.ZERO;
                String[] studentParts = studentAnswer.split(",");
                int correctCount = 0;
                for (int i = 0; i < correctParts.length; i++) {
                    if (i < studentParts.length && correctParts[i].trim().equalsIgnoreCase(studentParts[i].trim())) {
                        correctCount++;
                    }
                }
                return fullScore.multiply(new BigDecimal(correctCount))
                        .divide(new BigDecimal(correctParts.length), 2, RoundingMode.HALF_UP);
            default:
                return BigDecimal.ZERO;
        }
    }
}
