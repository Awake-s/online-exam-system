package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.exam.common.exception.BusinessException;
import com.exam.dto.request.MarkingScoreRequest;
import com.exam.entity.*;
import com.exam.mapper.*;
import com.exam.service.MarkingService;
import com.exam.service.NotificationOptions;
import com.exam.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MarkingServiceImpl implements MarkingService {

    @Autowired private ExamExamMapper examMapper;
    @Autowired private ExamRecordMapper recordMapper;
    @Autowired private ExamAnswerMapper answerMapper;
    @Autowired private ExamQuestionMapper questionMapper;
    @Autowired private ExamPaperQuestionMapper paperQuestionMapper;
    @Autowired private SysUserMapper userMapper;
    @Autowired private EduClassMapper classMapper;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private NotificationService notificationService;
    @Autowired private SimpMessagingTemplate messagingTemplate;

    @Override
    public List<Map<String, Object>> getPendingList(Long examId, Long creatorId) {
        ExamExam exam = examMapper.selectById(examId);
        if (exam == null) throw new BusinessException("考试不存在");
        if (!exam.getCreatorId().equals(creatorId)) throw new BusinessException("无权操作");

        // 查询已交卷(2)、已批改(3)、缺考(4)的记录，教师可看到完整的学生情况
        List<ExamRecord> records = recordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, examId).in(ExamRecord::getStatus, 2, 3, 4));

        Map<Long, String> classMap = classMapper.selectList(null).stream()
                .collect(Collectors.toMap(EduClass::getId, EduClass::getClassName));

        List<Long> userIds = records.stream().map(ExamRecord::getUserId).distinct().collect(Collectors.toList());
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Collections.emptyMap() :
                userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, u -> u));

        return records.stream().map(r -> {
            SysUser user = userMap.get(r.getUserId());
            Map<String, Object> m = new HashMap<>();
            m.put("recordId", r.getId());
            m.put("userId", r.getUserId());
            m.put("realName", user != null ? user.getRealName() : "");
            m.put("className", user != null && user.getClassId() != null ? classMap.get(user.getClassId()) : "");
            m.put("submitTime", r.getSubmitTime());
            m.put("objectiveScore", r.getObjectiveScore());
            m.put("status", r.getStatus());
            // 状态名：已批改(3)、缺考(4)、待批改(2)
            String statusName = r.getStatus() == 3 ? "已批改" : r.getStatus() == 4 ? "缺考" : "待批改";
            m.put("statusName", statusName);
            m.put("subjectiveScore", r.getSubjectiveScore());
            m.put("totalScore", r.getTotalScore());

            // 判断是否为空白卷（无有效答案），用于教师端区分空白卷和正常待批改
            if (r.getStatus() == 2) {
                List<ExamAnswer> ans = answerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                        .eq(ExamAnswer::getRecordId, r.getId()));
                boolean allBlank = ans.isEmpty() || ans.stream()
                        .allMatch(a -> a.getAnswer() == null || a.getAnswer().trim().isEmpty());
                m.put("allBlank", allBlank);
                // 判断是否含主观题
                List<ExamPaperQuestion> pqItems = paperQuestionMapper.selectList(
                        new LambdaQueryWrapper<ExamPaperQuestion>().eq(ExamPaperQuestion::getPaperId, r.getPaperId()));
                boolean hasSub = pqItems.stream().anyMatch(pq -> {
                    ExamQuestion q = questionMapper.selectById(pq.getQuestionId());
                    return q != null && q.getQuestionType() == 5;
                });
                m.put("hasSubjective", hasSub);
            } else {
                m.put("allBlank", false);
                m.put("hasSubjective", false);
            }
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getMarkingDetail(Long recordId, Long creatorId) {
        ExamRecord record = recordMapper.selectById(recordId);
        if (record == null) throw new BusinessException("记录不存在");
        ExamExam exam = examMapper.selectById(record.getExamId());
        if (exam == null || !exam.getCreatorId().equals(creatorId)) throw new BusinessException("无权操作");

        SysUser user = userMapper.selectById(record.getUserId());
        Map<String, Object> result = new HashMap<>();
        result.put("recordId", record.getId());
        result.put("realName", user != null ? user.getRealName() : "");
        result.put("examName", exam.getExamName());
        result.put("submitTime", record.getSubmitTime());
        result.put("objectiveScore", record.getObjectiveScore());
        result.put("status", record.getStatus());

        List<ExamAnswer> answers = answerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                .eq(ExamAnswer::getRecordId, recordId));
        Map<Long, ExamAnswer> answerMap = answers.stream()
                .collect(Collectors.toMap(ExamAnswer::getQuestionId, a -> a, (a, b) -> a));

        List<ExamPaperQuestion> pqs = paperQuestionMapper.selectList(
                new LambdaQueryWrapper<ExamPaperQuestion>()
                        .eq(ExamPaperQuestion::getPaperId, record.getPaperId())
                        .orderByAsc(ExamPaperQuestion::getSortOrder));

        List<Map<String, Object>> answerList = new ArrayList<>();
        for (ExamPaperQuestion pq : pqs) {
            ExamQuestion q = questionMapper.selectById(pq.getQuestionId());
            if (q == null) continue;
            ExamAnswer a = answerMap.get(pq.getQuestionId());
            Map<String, Object> am = new HashMap<>();
            am.put("answerId", a != null ? a.getId() : null);
            am.put("questionId", pq.getQuestionId());
            am.put("questionType", q.getQuestionType());
            am.put("content", q.getContent());
            am.put("options", parseOptions(q.getOptions()));
            am.put("correctAnswer", q.getAnswer());
            am.put("studentAnswer", a != null ? a.getAnswer() : null);
            am.put("isCorrect", a != null ? a.getIsCorrect() : null);
            am.put("score", a != null ? a.getScore() : null);
            am.put("fullScore", pq.getScore());
            am.put("comment", a != null ? a.getComment() : null);
            am.put("analysis", q.getAnalysis());
            am.put("needMarking", q.getQuestionType() == 5);
            answerList.add(am);
        }
        result.put("answers", answerList);
        return result;
    }

    @Override
    @Transactional
    public void markScores(MarkingScoreRequest request, Long creatorId) {
        ExamRecord record = recordMapper.selectById(request.getRecordId());
        if (record == null) throw new BusinessException("记录不存在");
        ExamExam exam = examMapper.selectById(record.getExamId());
        if (exam == null || !exam.getCreatorId().equals(creatorId)) throw new BusinessException("无权操作");

        // 优化5：状态防护 - 仅status=2(待批改)和status=3(已批改/重新批改)允许提交批改
        // 缺考(4)和进行中(1)不允许批改
        if (record.getStatus() != 2 && record.getStatus() != 3) {
            throw new BusinessException("当前记录状态不允许批改");
        }

        List<ExamPaperQuestion> pqs = paperQuestionMapper.selectList(
                new LambdaQueryWrapper<ExamPaperQuestion>().eq(ExamPaperQuestion::getPaperId, record.getPaperId()));
        Map<Long, BigDecimal> fullScoreMap = new HashMap<>();
        for (ExamPaperQuestion pq : pqs) {
            fullScoreMap.put(pq.getQuestionId(), pq.getScore());
        }

        for (MarkingScoreRequest.ScoreItem item : request.getScores()) {
            ExamAnswer answer = answerMapper.selectById(item.getAnswerId());
            if (answer == null) continue;

            if (!answer.getRecordId().equals(request.getRecordId())) {
                throw new BusinessException("答案不属于当前考试记录");
            }

            if (item.getScore().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("给分不能为负数");
            }

            BigDecimal fullScore = fullScoreMap.getOrDefault(answer.getQuestionId(), BigDecimal.ZERO);
            if (item.getScore().compareTo(fullScore) > 0) {
                throw new BusinessException("给分不能超过满分" + fullScore);
            }

            answer.setScore(item.getScore());
            answer.setComment(item.getComment());
            if (item.getScore().compareTo(fullScore) == 0) answer.setIsCorrect(1);
            else if (item.getScore().compareTo(BigDecimal.ZERO) == 0) answer.setIsCorrect(0);
            else answer.setIsCorrect(2);
            answerMapper.updateById(answer);
        }

        List<ExamAnswer> allAnswers = answerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                .eq(ExamAnswer::getRecordId, record.getId()));
        BigDecimal subjectiveScore = BigDecimal.ZERO;
        int subjectiveTotal = 0;
        int subjectiveMarked = 0;
        for (ExamAnswer a : allAnswers) {
            ExamQuestion q = questionMapper.selectById(a.getQuestionId());
            if (q != null && q.getQuestionType() == 5) {
                subjectiveTotal++;
                if (a.getIsCorrect() != null) {
                    subjectiveMarked++;
                }
                subjectiveScore = subjectiveScore.add(a.getScore() != null ? a.getScore() : BigDecimal.ZERO);
            }
        }

        if (subjectiveTotal > 0 && subjectiveMarked < subjectiveTotal) {
            throw new BusinessException("还有 " + (subjectiveTotal - subjectiveMarked) + " 道主观题未批改，请全部批改后再提交");
        }

        record.setSubjectiveScore(subjectiveScore);
        record.setTotalScore((record.getObjectiveScore() != null ? record.getObjectiveScore() : BigDecimal.ZERO).add(subjectiveScore));
        record.setStatus(3);
        recordMapper.updateById(record);
    }

    /**
     * 发布成绩（支持无限次重发）。
     *
     * <p>设计参考：Canvas LMS / Moodle / Open edX / GitLab Release —— 允许教师多次修正后重新发布。
     * <ul>
     *   <li>前置校验：存在性、权限、考试已结束、无待批改。</li>
     *   <li>用 {@code last_publish_time} 作为乐观锁版本号做 CAS 更新，防止并发双击产生多条通知。</li>
     *   <li>首次发布 vs 重新发布：通知 type/title/content 差异化。</li>
     *   <li>重发场景下通知按 10 分钟窗口去重，避免短时间内多次修正刷屏。</li>
     *   <li>通知与 WebSocket 推送延迟到事务提交后执行，保证数据一致性。</li>
     * </ul>
     */
    @Override
    @Transactional
    public void publishScores(Long examId, Long creatorId) {
        ExamExam exam = examMapper.selectById(examId);
        if (exam == null) throw new BusinessException("考试不存在");
        if (!exam.getCreatorId().equals(creatorId)) throw new BusinessException("无权操作");

        // 考试尚未结束，禁止发布成绩（业界标准：成绩须在考试结束后统一发布）
        if (exam.getEndTime() != null && exam.getEndTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException("考试尚未结束，请在考试结束后再发布成绩");
        }

        // 校验是否还有待批改的记录（status=2），全部批改完成后才允许发布
        long pendingCount = recordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, examId).eq(ExamRecord::getStatus, 2));
        if (pendingCount > 0) {
            throw new BusinessException("还有 " + pendingCount + " 份试卷未批改，请全部批改完成后再发布成绩");
        }

        // 查询已批改(status=3)和缺考(status=4)的记录，作为通知对象
        List<ExamRecord> records = recordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, examId).in(ExamRecord::getStatus, 3, 4));
        if (records.isEmpty()) {
            throw new BusinessException("没有已批改的记录可发布");
        }

        // 用 last_publish_time 作为乐观锁版本号做 CAS 更新：
        //   - 首次发布：WHERE score_published = 0 AND last_publish_time IS NULL
        //   - 重新发布：WHERE last_publish_time = <读到的旧值>
        // 并发双击时第二次 CAS 必然失败（受影响行数为 0），自然防止重复通知
        LocalDateTime lastPublishTime = exam.getLastPublishTime();
        boolean isFirstPublish = (lastPublishTime == null);
        LocalDateTime now = LocalDateTime.now();

        LambdaUpdateWrapper<ExamExam> casWrapper = new LambdaUpdateWrapper<ExamExam>()
                .eq(ExamExam::getId, examId)
                .set(ExamExam::getScorePublished, 1)
                .set(ExamExam::getLastPublishTime, now);
        if (isFirstPublish) {
            casWrapper.isNull(ExamExam::getLastPublishTime);
        } else {
            casWrapper.eq(ExamExam::getLastPublishTime, lastPublishTime);
        }
        int affected = examMapper.update(null, casWrapper);
        if (affected == 0) {
            // 并发双击或页面数据陈旧，另一条请求已抢先处理
            throw new BusinessException("成绩正在发布中，请稍候刷新后重试");
        }

        // 通知与 WebSocket 推送延迟到事务提交后执行，避免事务回滚后通知已外发导致数据不一致
        final String examName = exam.getExamName();
        final List<ExamRecord> finalRecords = records;
        final boolean finalIsFirst = isFirstPublish;
        // E1: 提前查教师信息，作为通知发送者
        final SysUser teacher = userMapper.selectById(creatorId);
        final String teacherName = teacher != null ? teacher.getRealName() : "教师";
        final String teacherAvatar = teacher != null ? teacher.getAvatar() : null;
        final Long teacherId = creatorId;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // 事务已提交，通知失败不影响主业务；仅记录日志
                    try {
                        sendPublishNotifications(examId, examName, finalRecords, finalIsFirst,
                                teacherId, teacherName, teacherAvatar);
                    } catch (Exception e) {
                        log.error("发布成绩后发送通知失败 examId={} examName={} isFirstPublish={}",
                                examId, examName, finalIsFirst, e);
                    }
                }
            });
        } else {
            try {
                sendPublishNotifications(examId, examName, finalRecords, finalIsFirst,
                        teacherId, teacherName, teacherAvatar);
            } catch (Exception e) {
                log.error("发布成绩后发送通知失败（无事务上下文）examId={}", examId, e);
            }
        }
    }

    /**
     * 发布成绩后通知推送（含站内信 + WebSocket 实时消息）。
     * <ul>
     *   <li>首次发布：type=SCORE_PUBLISHED，标题"成绩发布"，直接新建通知</li>
     *   <li>重新发布：type=SCORE_UPDATED，标题"成绩已更新"，10 分钟内合并相同学生的通知</li>
     * </ul>
     */
    private void sendPublishNotifications(Long examId, String examName,
                                          List<ExamRecord> records, boolean isFirstPublish,
                                          Long teacherId, String teacherName, String teacherAvatar) {
        final String type = isFirstPublish ? "SCORE_PUBLISHED" : "SCORE_UPDATED";
        final String titlePrefix = isFirstPublish ? "成绩发布：" : "成绩已更新：";
        final int dedupeWindowMinutes = 10;

        for (ExamRecord r : records) {
            String title = titlePrefix + examName;
            String baseContent = r.getStatus() == 4
                    ? "你被标记为缺考，成绩为 0 分"
                    : "你的得分：" + r.getTotalScore() + " 分";
            String content = isFirstPublish
                    ? baseContent
                    : baseContent + "（教师已对成绩进行修正，请查看最新结果）";

            // E1: 成绩通知携带教师发送者信息
            NotificationOptions opts = NotificationOptions.defaults()
                    .withSender(teacherId, teacherName, teacherAvatar);

            if (isFirstPublish) {
                notificationService.notifyUser(r.getUserId(), type, title, content, "score", examId, opts);
            } else {
                // 重发场景：10 分钟内同学生同考试的 SCORE_UPDATED 通知合并为最新一条
                notificationService.notifyUserWithDedupe(r.getUserId(), type, title, content,
                        "score", examId, dedupeWindowMinutes, opts);
            }

            Map<String, Object> wsEvent = new HashMap<>();
            wsEvent.put("type", type);
            wsEvent.put("examId", examId);
            wsEvent.put("examName", examName);
            wsEvent.put("totalScore", r.getTotalScore());
            wsEvent.put("isUpdate", !isFirstPublish);
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(r.getUserId()),
                    "/queue/exam-events", wsEvent);
        }
    }

    private List<String> parseOptions(String json) {
        if (json == null || json.isEmpty()) return null;
        try { return objectMapper.readValue(json, List.class); }
        catch (Exception e) { return null; }
    }
}
