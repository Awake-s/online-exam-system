package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.common.exception.BusinessException;
import com.exam.dto.request.ExamSubmitRequest;
import com.exam.entity.*;
import com.exam.mapper.*;
import com.exam.service.NotificationOptions;
import com.exam.service.NotificationService;
import com.exam.service.StudentExamService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StudentExamServiceImpl implements StudentExamService {

    @Autowired private ExamExamMapper examMapper;
    @Autowired private ExamPaperMapper paperMapper;
    @Autowired private ExamPaperQuestionMapper paperQuestionMapper;
    @Autowired private ExamQuestionMapper questionMapper;
    @Autowired private ExamRecordMapper recordMapper;
    @Autowired private ExamAnswerMapper answerMapper;
    @Autowired private SysUserMapper userMapper;
    @Autowired private EduSubjectMapper subjectMapper;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private NotificationService notificationService;
    @Autowired private SimpMessagingTemplate messagingTemplate;

    private static final String[] RECORD_STATUS_NAMES = {"未开始", "答题中", "已交卷", "已批改", "缺考"};

    @Override
    public List<Map<String, Object>> getMyExams(Long userId, Long classId) {
        if (classId == null) return Collections.emptyList();
        SysUser currentUser = userMapper.selectById(userId);
        if (currentUser == null) return Collections.emptyList();
        LambdaQueryWrapper<ExamExam> examWrapper = new LambdaQueryWrapper<ExamExam>()
                .eq(ExamExam::getClassId, classId).orderByDesc(ExamExam::getStartTime);
        if (currentUser.getCreateTime() != null) {
            examWrapper.ge(ExamExam::getStartTime, currentUser.getCreateTime());
        }
        List<ExamExam> exams = examMapper.selectList(examWrapper);

        LocalDateTime now = LocalDateTime.now();
        return exams.stream().map(e -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", e.getId());
            m.put("examName", e.getExamName());

            ExamPaper paper = paperMapper.selectById(e.getPaperId());
            if (paper != null) {
                EduSubject subject = subjectMapper.selectById(paper.getSubjectId());
                m.put("subjectName", subject != null ? subject.getSubjectName() : "");
                m.put("duration", paper.getDuration());
                m.put("totalScore", paper.getTotalScore());
            }
            m.put("startTime", e.getStartTime());
            m.put("endTime", e.getEndTime());

            int status;
            if (now.isBefore(e.getStartTime())) status = 0;
            else if (now.isAfter(e.getEndTime())) status = 2;
            else status = 1;
            m.put("status", status);
            String[] statusNames = {"未开始", "进行中", "已结束"};
            m.put("statusName", statusNames[status]);

            ExamRecord record = recordMapper.selectOne(new LambdaQueryWrapper<ExamRecord>()
                    .eq(ExamRecord::getExamId, e.getId()).eq(ExamRecord::getUserId, userId));
            boolean published = Integer.valueOf(1).equals(e.getScorePublished());
            if (record != null) {
                Integer recordStatus = record.getStatus();
                // 成绩未发布时，将"已批改(3)"伪装为"已交卷(2)"，学生无法感知批改进度
                if (recordStatus != null && recordStatus == 3 && !published) {
                    recordStatus = 2;
                }
                // 缺考(4)是客观事实，始终如实展示，不需要等成绩发布
                m.put("recordStatus", recordStatus);
                m.put("scorePublished", published && record.getStatus() >= 3);
                m.put("recordStatusName", recordStatus != null && recordStatus >= 0 && recordStatus <= 4
                        ? RECORD_STATUS_NAMES[recordStatus] : "");
                m.put("recordId", record.getId());
                m.put("score", (record.getStatus() >= 3 && published) ? record.getTotalScore() : null);
            } else {
                m.put("recordStatus", null);
                m.put("scorePublished", false);
                m.put("recordStatusName", null);
                m.put("recordId", null);
                m.put("score", null);
            }
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Map<String, Object> startExam(Long examId, Long userId) {
        ExamExam exam = examMapper.selectById(examId);
        if (exam == null) throw new BusinessException("考试不存在");

        SysUser user = userMapper.selectById(userId);
        if (user == null || !exam.getClassId().equals(user.getClassId())) {
            throw new BusinessException("您不属于该考试班级");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(exam.getStartTime()) || now.isAfter(exam.getEndTime())) {
            throw new BusinessException("考试未开始或已结束");
        }

        ExamRecord existRecord = recordMapper.selectOne(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, examId).eq(ExamRecord::getUserId, userId));
        if (existRecord != null) {
            if (existRecord.getStatus() >= 2) throw new BusinessException("您已完成该考试");
            return buildExamData(existRecord, exam);
        }

        ExamRecord record = new ExamRecord();
        record.setExamId(examId);
        record.setUserId(userId);
        record.setPaperId(exam.getPaperId());
        record.setStartTime(now);
        record.setStatus(1);
        try {
            recordMapper.insert(record);
        } catch (DuplicateKeyException e) {
            // 并发场景：同一学生多个端同时进入考试，唯一约束 uk_exam_user 触发冲突。
            // 改为查询已存在的记录返回，避免 500 错误。
            ExamRecord existed = recordMapper.selectOne(new LambdaQueryWrapper<ExamRecord>()
                    .eq(ExamRecord::getExamId, examId).eq(ExamRecord::getUserId, userId));
            if (existed == null) throw new BusinessException("创建考试记录失败，请重试");
            if (existed.getStatus() >= 2) throw new BusinessException("您已完成该考试");
            record = existed;
        }

        return buildExamData(record, exam);
    }

    private Map<String, Object> buildExamData(ExamRecord record, ExamExam exam) {
        ExamPaper paper = paperMapper.selectById(exam.getPaperId());
        List<ExamPaperQuestion> pqs = paperQuestionMapper.selectList(
                new LambdaQueryWrapper<ExamPaperQuestion>()
                        .eq(ExamPaperQuestion::getPaperId, exam.getPaperId())
                        .orderByAsc(ExamPaperQuestion::getSortOrder));

        // 解析防作弊配置
        Map<String, Object> antiCheat = parseAntiCheatConfig(exam.getAntiCheatConfig());
        boolean shuffleQuestion = Boolean.TRUE.equals(antiCheat.get("shuffleQuestion"));
        boolean shuffleOption = Boolean.TRUE.equals(antiCheat.get("shuffleOption"));

        List<Map<String, Object>> questions = new ArrayList<>();
        Map<Long, List<String>> shuffledQOrigOpts = new HashMap<>();
        for (ExamPaperQuestion pq : pqs) {
            ExamQuestion q = questionMapper.selectById(pq.getQuestionId());
            if (q == null) continue;
            Map<String, Object> qm = new HashMap<>();
            qm.put("id", q.getId());
            qm.put("questionType", q.getQuestionType());
            qm.put("content", q.getContent());
            List<String> opts = parseOptions(q.getOptions());
            // 选项乱序：仅对选择题(1单选 2多选)生效，使用recordId作为稳定种子
            if (shuffleOption && opts != null && !opts.isEmpty()
                    && (q.getQuestionType() == 1 || q.getQuestionType() == 2)) {
                shuffledQOrigOpts.put(q.getId(), new ArrayList<>(opts));
                opts = shuffleWithSeed(opts, record.getId() + q.getId());
                opts = relabelOptions(opts);
            }
            qm.put("options", opts);
            qm.put("score", pq.getScore());
            qm.put("sortOrder", pq.getSortOrder());
            questions.add(qm);
        }

        // 题目乱序：按题型分组后组内乱序，避免题型分组标题碎片化
        if (shuffleQuestion) {
            Map<Integer, List<Map<String, Object>>> typeGroups = new LinkedHashMap<>();
            for (Map<String, Object> qm : questions) {
                int qType = (int) qm.get("questionType");
                typeGroups.computeIfAbsent(qType, k -> new ArrayList<>()).add(qm);
            }
            questions = new ArrayList<>();
            int groupSeed = 0;
            for (List<Map<String, Object>> group : typeGroups.values()) {
                questions.addAll(shuffleWithSeed(group, record.getId() + groupSeed));
                groupSeed++;
            }
        }

        // 查询已保存的答案（断线续答场景）
        List<ExamAnswer> savedAnswers = answerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                .eq(ExamAnswer::getRecordId, record.getId()));
        Map<Long, String> savedAnswerMap = new HashMap<>();
        for (ExamAnswer a : savedAnswers) {
            savedAnswerMap.put(a.getQuestionId(), a.getAnswer());
        }
        // 选项乱序时将已保存的原始答案转换为新标签（断线续答：选项已重标为ABCD，答案也要对应转换）
        for (Map.Entry<Long, String> entry : savedAnswerMap.entrySet()) {
            List<String> origOpts = shuffledQOrigOpts.get(entry.getKey());
            if (origOpts != null && entry.getValue() != null && !entry.getValue().isEmpty()) {
                entry.setValue(convertOriginalToNew(entry.getValue(), origOpts, record.getId() + entry.getKey()));
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("recordId", record.getId());
        result.put("examName", exam.getExamName());
        result.put("duration", paper != null ? paper.getDuration() : 120);
        result.put("endTime", exam.getEndTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        result.put("serverTime", System.currentTimeMillis());
        result.put("questions", questions);
        result.put("savedAnswers", savedAnswerMap);
        result.put("antiCheat", antiCheat);
        result.put("switchCount", record.getSwitchCount() != null ? record.getSwitchCount() : 0);
        return result;
    }

    private Map<String, Object> parseAntiCheatConfig(String json) {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("switchScreenMax", 0);
        defaults.put("shuffleQuestion", false);
        defaults.put("shuffleOption", false);
        defaults.put("fullscreenRequired", false);
        defaults.put("noCopyPaste", false);
        defaults.put("inactivityTimeout", 0);
        if (json == null || json.isEmpty()) return defaults;
        try {
            Map<String, Object> parsed = objectMapper.readValue(json, Map.class);
            for (Map.Entry<String, Object> entry : parsed.entrySet()) {
                defaults.put(entry.getKey(), entry.getValue());
            }
        } catch (Exception ignored) {}
        return defaults;
    }

    private <T> List<T> shuffleWithSeed(List<T> list, long seed) {
        List<T> result = new ArrayList<>(list);
        Collections.shuffle(result, new Random(seed));
        return result;
    }

    // 选项乱序后重新标记为顺序 A、B、C、D，避免前端显示标签错乱
    private List<String> relabelOptions(List<String> shuffledOpts) {
        String[] labels = {"A", "B", "C", "D", "E", "F", "G", "H"};
        List<String> result = new ArrayList<>();
        for (int i = 0; i < shuffledOpts.size(); i++) {
            String content = shuffledOpts.get(i).length() > 2 ? shuffledOpts.get(i).substring(2) : shuffledOpts.get(i);
            result.add(labels[i] + "." + content);
        }
        return result;
    }

    // 构建乱序映射：新顺序标签(A,B,C,D) → 原始字母
    private Map<String, String> buildShuffleMapping(List<String> originalOpts, long seed) {
        List<String> shuffled = shuffleWithSeed(originalOpts, seed);
        String[] labels = {"A", "B", "C", "D", "E", "F", "G", "H"};
        Map<String, String> newToOriginal = new HashMap<>();
        for (int i = 0; i < shuffled.size(); i++) {
            newToOriginal.put(labels[i], shuffled.get(i).substring(0, 1).toUpperCase());
        }
        return newToOriginal;
    }

    // 将学生答案从新标签转换回原始字母（用于存储和评分）
    private String convertShuffledAnswer(String answer, List<String> originalOpts, long seed) {
        if (answer == null || answer.trim().isEmpty()) return answer;
        Map<String, String> mapping = buildShuffleMapping(originalOpts, seed);
        return Arrays.stream(answer.split(","))
                .map(s -> mapping.getOrDefault(s.trim().toUpperCase(), s.trim()))
                .collect(Collectors.joining(","));
    }

    // 将原始字母转换为新标签（断线续答时回显已保存答案）
    private String convertOriginalToNew(String answer, List<String> originalOpts, long seed) {
        if (answer == null || answer.trim().isEmpty()) return answer;
        Map<String, String> newToOrig = buildShuffleMapping(originalOpts, seed);
        Map<String, String> origToNew = new HashMap<>();
        newToOrig.forEach((k, v) -> origToNew.put(v, k));
        return Arrays.stream(answer.split(","))
                .map(s -> origToNew.getOrDefault(s.trim().toUpperCase(), s.trim()))
                .collect(Collectors.joining(","));
    }

    @Override
    @Transactional
    public Map<String, Object> submitExam(ExamSubmitRequest request, Long userId) {
        // 并发防护：先介于 @Transactional + status>=2 预检查，
        // 再通过 CAS 更新（仅当 status=1 才更新）免除与 ExamEndTask 并发时的覆盖写。
        ExamRecord record = recordMapper.selectById(request.getRecordId());
        if (record == null) throw new BusinessException("考试记录不存在");
        if (!record.getUserId().equals(userId)) throw new BusinessException("无权操作");
        if (record.getStatus() >= 2) throw new BusinessException("试卷已提交");

        ExamExam exam = examMapper.selectById(record.getExamId());
        if (exam != null && LocalDateTime.now().isAfter(exam.getEndTime().plusSeconds(60))) {
            throw new BusinessException("考试已结束，无法提交");
        }
        Map<String, Object> antiCheatCfg = parseAntiCheatConfig(exam != null ? exam.getAntiCheatConfig() : null);
        boolean shuffleOpt = Boolean.TRUE.equals(antiCheatCfg.get("shuffleOption"));

        List<ExamPaperQuestion> pqs = paperQuestionMapper.selectList(
                new LambdaQueryWrapper<ExamPaperQuestion>().eq(ExamPaperQuestion::getPaperId, record.getPaperId()));
        Map<Long, ExamPaperQuestion> pqMap = pqs.stream()
                .collect(Collectors.toMap(ExamPaperQuestion::getQuestionId, pq -> pq));

        BigDecimal objectiveScore = BigDecimal.ZERO;
        boolean allBlank = true;

        // 从试卷题目推断是否包含主观题（更健壮，不依赖前端传递的答案列表）
        boolean hasSubjective = false;
        for (ExamPaperQuestion pqItem : pqs) {
            ExamQuestion q = questionMapper.selectById(pqItem.getQuestionId());
            if (q != null && q.getQuestionType() == 5) {
                hasSubjective = true;
                break;
            }
        }

        List<ExamAnswer> existingAnswers = answerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                .eq(ExamAnswer::getRecordId, record.getId()));
        Map<Long, ExamAnswer> existingMap = existingAnswers.stream()
                .collect(Collectors.toMap(ExamAnswer::getQuestionId, a -> a, (a, b) -> a));

        if (request.getAnswers() != null) {
            for (ExamSubmitRequest.AnswerItem item : request.getAnswers()) {
                if (item.getAnswer() != null && !item.getAnswer().trim().isEmpty()) {
                    allBlank = false;
                    break;
                }
            }
        }

        if (request.getAnswers() != null) {
            for (ExamSubmitRequest.AnswerItem item : request.getAnswers()) {
                ExamQuestion question = questionMapper.selectById(item.getQuestionId());
                ExamPaperQuestion pq = pqMap.get(item.getQuestionId());
                if (question == null || pq == null) continue;

                BigDecimal fullScore = pq.getScore();
                int type = question.getQuestionType();
                BigDecimal scored = BigDecimal.ZERO;
                Integer isCorrect = null;

                // 选项乱序时将答案从新标签转换回原始字母
                String actualAnswer = item.getAnswer();
                if (shuffleOpt && (type == 1 || type == 2)) {
                    List<String> originalOpts = parseOptions(question.getOptions());
                    if (originalOpts != null && !originalOpts.isEmpty()) {
                        actualAnswer = convertShuffledAnswer(actualAnswer, originalOpts, record.getId() + question.getId());
                    }
                }

                if (type == 5) {
                    // 主观题跳过自动评分
                } else {
                    scored = autoGrade(type, actualAnswer, question.getAnswer(), fullScore);
                    if (scored.compareTo(fullScore) == 0) {
                        isCorrect = 1;
                    } else if (scored.compareTo(BigDecimal.ZERO) == 0) {
                        isCorrect = 0;
                    } else {
                        isCorrect = 2;
                    }
                    objectiveScore = objectiveScore.add(scored);
                }

                ExamAnswer existing = existingMap.get(item.getQuestionId());
                if (existing != null) {
                    existing.setAnswer(actualAnswer);
                    existing.setIsRemoved(0);
                    existing.setScore(scored);
                    existing.setIsCorrect(isCorrect);
                    answerMapper.updateById(existing);
                    existingMap.remove(item.getQuestionId());
                } else {
                    ExamAnswer answer = new ExamAnswer();
                    answer.setRecordId(record.getId());
                    answer.setQuestionId(item.getQuestionId());
                    answer.setAnswer(actualAnswer);
                    answer.setIsRemoved(0);
                    answer.setScore(scored);
                    answer.setIsCorrect(isCorrect);
                    answerMapper.insert(answer);
                }
            }
        }
        // 清理提交时不再包含的旧答案
        for (ExamAnswer leftover : existingMap.values()) {
            answerMapper.deleteById(leftover.getId());
        }

        LocalDateTime submitTime = LocalDateTime.now();
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
        int affected = recordMapper.casSubmitFromInProgress(record.getId(), newStatus, submitTime,
                objectiveScore, subjectiveForUpdate, totalForUpdate);
        if (affected == 0) {
            // 并发要求下被 ExamEndTask 抢先自动交卷，提示不要重复提交
            throw new BusinessException("试卷已提交");
        }
        // 同步内存对象便于后续使用
        record.setSubmitTime(submitTime);
        record.setObjectiveScore(objectiveScore);
        record.setSubjectiveScore(subjectiveForUpdate);
        record.setTotalScore(totalForUpdate);
        record.setStatus(newStatus);

        Map<String, Object> result = new HashMap<>();
        result.put("recordId", record.getId());
        result.put("objectiveScore", objectiveScore);
        result.put("hasSubjective", hasSubjective);
        result.put("message", hasSubjective ? "客观题得分" + objectiveScore + "分，主观题待教师批改" : "考试完成，总分" + objectiveScore + "分");

        // 通知与 WebSocket 推送延迟到事务提交后发送，避免事务回滚后外部通知已发出导致不一致
        final ExamExam examEntity = examMapper.selectById(record.getExamId());
        if (examEntity != null) {
            final SysUser student = userMapper.selectById(userId);
            final String studentName = student != null ? student.getRealName() : "学生";
            final String studentAvatar = student != null ? student.getAvatar() : null;
            final Long studentId = userId;
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        // 事务已提交，通知失败不影响主业务；仅记录日志
                        try {
                            sendSubmitNotifications(examEntity, studentId, studentName, studentAvatar);
                        } catch (Exception e) {
                            log.error("交卷后发送通知失败 examId={} studentId={} studentName={}",
                                    examEntity.getId(), studentId, studentName, e);
                        }
                    }
                });
            } else {
                try {
                    sendSubmitNotifications(examEntity, studentId, studentName, studentAvatar);
                } catch (Exception e) {
                    log.error("交卷后发送通知失败（无事务上下文）examId={}", examEntity.getId(), e);
                }
            }
        }

        return result;
    }

    /**
     * 学生交卷后通知推送（站内信 + WebSocket 实时消息）。
     * 单独抽取以便事务提交后调用，保证通知与 DB 状态一致。
     * <p>E1: 携带学生作为发送者信息（老师能看到是谁交卷的头像和姓名）
     */
    private void sendSubmitNotifications(ExamExam examEntity, Long studentId, String studentName, String studentAvatar) {
        notificationService.notifyUser(examEntity.getCreatorId(), "EXAM_SUBMITTED",
                studentName + " 已提交考试",
                "考试：" + examEntity.getExamName(),
                "exam", examEntity.getId(),
                NotificationOptions.defaults().withSender(studentId, studentName, studentAvatar));

        Map<String, Object> wsEvent = new HashMap<>();
        wsEvent.put("type", "EXAM_SUBMITTED");
        wsEvent.put("examId", examEntity.getId());
        wsEvent.put("studentName", studentName);
        wsEvent.put("examName", examEntity.getExamName());
        messagingTemplate.convertAndSendToUser(
                String.valueOf(examEntity.getCreatorId()),
                "/queue/exam-events", wsEvent);
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

    @Override
    public Map<String, Object> getExamResult(Long recordId, Long userId) {
        ExamRecord record = recordMapper.selectById(recordId);
        if (record == null) throw new BusinessException("记录不存在");
        if (!record.getUserId().equals(userId)) throw new BusinessException("无权查看");
        if (record.getStatus() < 3) throw new BusinessException("成绩尚未发布，请等待教师批改");

        ExamExam exam = examMapper.selectById(record.getExamId());
        if (exam != null && !Integer.valueOf(1).equals(exam.getScorePublished())) {
            throw new BusinessException("成绩尚未发布，请等待教师发布成绩");
        }
        ExamPaper paper = paperMapper.selectById(record.getPaperId());

        Map<String, Object> result = new HashMap<>();
        result.put("recordId", record.getId());
        result.put("examName", exam != null ? exam.getExamName() : "");
        result.put("totalScore", record.getTotalScore());
        result.put("objectiveScore", record.getObjectiveScore());
        result.put("subjectiveScore", record.getSubjectiveScore());
        result.put("paperTotalScore", paper != null ? paper.getTotalScore() : BigDecimal.ZERO);
        result.put("passScore", paper != null ? paper.getPassScore() : BigDecimal.ZERO);
        boolean isPassed = record.getTotalScore() != null && paper != null &&
                record.getTotalScore().compareTo(paper.getPassScore()) >= 0;
        result.put("isPassed", isPassed);
        result.put("status", record.getStatus());
        Integer recordStatus = record.getStatus();
        result.put("statusName", recordStatus != null && recordStatus >= 0 && recordStatus <= 4
                ? RECORD_STATUS_NAMES[recordStatus] : "");
        result.put("submitTime", record.getSubmitTime());

        // 排名计算：竞赛排名（1,1,3），与教师端 getClassScores 保持一致。
        if (record.getTotalScore() != null && exam != null) {
            long higherCount = recordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>()
                    .eq(ExamRecord::getExamId, record.getExamId())
                    .in(ExamRecord::getStatus, 3, 4)
                    .gt(ExamRecord::getTotalScore, record.getTotalScore()));
            long rank = higherCount + 1;
            long totalParticipants = recordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>()
                    .eq(ExamRecord::getExamId, record.getExamId())
                    .in(ExamRecord::getStatus, 3, 4));
            result.put("rank", rank);
            result.put("totalParticipants", totalParticipants);
        } else {
            result.put("rank", null);
            result.put("totalParticipants", null);
        }

        List<ExamAnswer> answers = answerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                .eq(ExamAnswer::getRecordId, recordId));
        Map<Long, ExamAnswer> answerMap = answers.stream()
                .collect(Collectors.toMap(ExamAnswer::getQuestionId, a -> a, (a, b) -> a));
        List<ExamPaperQuestion> pqs = paperQuestionMapper.selectList(
                new LambdaQueryWrapper<ExamPaperQuestion>()
                        .eq(ExamPaperQuestion::getPaperId, record.getPaperId())
                        .orderByAsc(ExamPaperQuestion::getSortOrder));
        String[] typeNames = {"", "单选题", "多选题", "判断题", "填空题", "简答题"};

        List<Map<String, Object>> answerList = new ArrayList<>();
        for (ExamPaperQuestion pq : pqs) {
            ExamQuestion q = questionMapper.selectById(pq.getQuestionId());
            if (q == null) continue;
            ExamAnswer a = answerMap.get(pq.getQuestionId());
            Map<String, Object> am = new HashMap<>();
            am.put("questionId", pq.getQuestionId());
            am.put("questionType", q.getQuestionType());
            am.put("questionTypeName", q.getQuestionType() <= 5 ? typeNames[q.getQuestionType()] : "");
            am.put("content", q.getContent());
            am.put("options", parseOptions(q.getOptions()));
            am.put("correctAnswer", q.getAnswer());
            am.put("myAnswer", a != null ? a.getAnswer() : null);
            am.put("isCorrect", a != null ? a.getIsCorrect() : null);
            am.put("score", a != null ? a.getScore() : null);
            am.put("fullScore", pq.getScore());
            am.put("analysis", q.getAnalysis());
            am.put("comment", a != null ? a.getComment() : null);
            answerList.add(am);
        }
        result.put("answers", answerList);
        return result;
    }

    @Override
    @Transactional
    public void autoSaveAnswers(ExamSubmitRequest request, Long userId) {
        ExamRecord record = recordMapper.selectById(request.getRecordId());
        if (record == null || !record.getUserId().equals(userId)) return;
        if (record.getStatus() >= 2) return;

        ExamExam exam = examMapper.selectById(record.getExamId());
        if (exam != null && LocalDateTime.now().isAfter(exam.getEndTime().plusSeconds(60))) return;
        Map<String, Object> antiCheatCfg2 = parseAntiCheatConfig(exam != null ? exam.getAntiCheatConfig() : null);
        boolean shuffleOpt2 = Boolean.TRUE.equals(antiCheatCfg2.get("shuffleOption"));

        List<ExamPaperQuestion> pqs = paperQuestionMapper.selectList(
                new LambdaQueryWrapper<ExamPaperQuestion>().eq(ExamPaperQuestion::getPaperId, record.getPaperId()));
        Map<Long, ExamPaperQuestion> pqMap = pqs.stream()
                .collect(Collectors.toMap(ExamPaperQuestion::getQuestionId, pq -> pq));

        List<ExamAnswer> existingAnswers = answerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                .eq(ExamAnswer::getRecordId, record.getId()));
        Map<Long, ExamAnswer> existingMap = existingAnswers.stream()
                .collect(Collectors.toMap(ExamAnswer::getQuestionId, a -> a, (a, b) -> a));

        if (request.getAnswers() != null) {
            for (ExamSubmitRequest.AnswerItem item : request.getAnswers()) {
                if (item.getAnswer() == null || item.getAnswer().trim().isEmpty()) continue;

                ExamQuestion question = questionMapper.selectById(item.getQuestionId());
                ExamPaperQuestion pq = pqMap.get(item.getQuestionId());
                if (question == null || pq == null) continue;

                BigDecimal fullScore = pq.getScore();
                int type = question.getQuestionType();

                // 选项乱序时将答案从新标签转换回原始字母
                String actualAns = item.getAnswer();
                if (shuffleOpt2 && (type == 1 || type == 2)) {
                    List<String> originalOpts = parseOptions(question.getOptions());
                    if (originalOpts != null && !originalOpts.isEmpty()) {
                        actualAns = convertShuffledAnswer(actualAns, originalOpts, record.getId() + question.getId());
                    }
                }

                ExamAnswer existing = existingMap.get(item.getQuestionId());
                if (existing != null) {
                    existing.setAnswer(actualAns);
                    existing.setIsRemoved(0);
                    if (type == 5) {
                        existing.setIsCorrect(null);
                        existing.setScore(BigDecimal.ZERO);
                    } else {
                        BigDecimal scored = autoGrade(type, actualAns, question.getAnswer(), fullScore);
                        existing.setScore(scored);
                        if (scored.compareTo(fullScore) == 0) existing.setIsCorrect(1);
                        else if (scored.compareTo(BigDecimal.ZERO) == 0) existing.setIsCorrect(0);
                        else existing.setIsCorrect(2);
                    }
                    answerMapper.updateById(existing);
                } else {
                    ExamAnswer answer = new ExamAnswer();
                    answer.setRecordId(record.getId());
                    answer.setQuestionId(item.getQuestionId());
                    answer.setAnswer(actualAns);
                    answer.setIsRemoved(0);
                    if (type == 5) {
                        answer.setIsCorrect(null);
                        answer.setScore(BigDecimal.ZERO);
                    } else {
                        BigDecimal scored = autoGrade(type, actualAns, question.getAnswer(), fullScore);
                        answer.setScore(scored);
                        if (scored.compareTo(fullScore) == 0) answer.setIsCorrect(1);
                        else if (scored.compareTo(BigDecimal.ZERO) == 0) answer.setIsCorrect(0);
                        else answer.setIsCorrect(2);
                    }
                    answerMapper.insert(answer);
                }
            }
        }
    }

    @Override
    @Transactional
    public Map<String, Object> recordSwitchScreen(Long recordId, Long userId) {
        ExamRecord record = recordMapper.selectById(recordId);
        if (record == null || !record.getUserId().equals(userId)) throw new BusinessException("无权操作");
        if (record.getStatus() >= 2) throw new BusinessException("考试已结束");

        // 原子递增切屏计数，避免并发竞态
        recordMapper.incrementSwitchCount(recordId);
        record = recordMapper.selectById(recordId);
        int newCount = record.getSwitchCount() != null ? record.getSwitchCount() : 1;

        ExamExam exam = examMapper.selectById(record.getExamId());
        Map<String, Object> antiCheat = parseAntiCheatConfig(exam != null ? exam.getAntiCheatConfig() : null);
        int maxSwitch = antiCheat.get("switchScreenMax") instanceof Number
                ? ((Number) antiCheat.get("switchScreenMax")).intValue() : 0;

        Map<String, Object> result = new HashMap<>();
        result.put("switchCount", newCount);
        result.put("switchScreenMax", maxSwitch);
        result.put("forceSubmit", maxSwitch > 0 && newCount >= maxSwitch);
        return result;
    }

    private List<String> parseOptions(String json) {
        if (json == null || json.isEmpty()) return null;
        try { return objectMapper.readValue(json, List.class); }
        catch (Exception e) { return null; }
    }
}
