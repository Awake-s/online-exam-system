package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.common.exception.BusinessException;
import com.exam.common.result.PageResult;
import com.exam.entity.*;
import com.exam.mapper.*;
import com.exam.service.WrongService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WrongServiceImpl implements WrongService {

    @Autowired private ExamAnswerMapper answerMapper;
    @Autowired private ExamRecordMapper recordMapper;
    @Autowired private ExamQuestionMapper questionMapper;
    @Autowired private ExamPaperQuestionMapper paperQuestionMapper;
    @Autowired private ExamExamMapper examMapper;
    @Autowired private EduSubjectMapper subjectMapper;
    @Autowired private ExamPaperMapper paperMapper;
    @Autowired private ObjectMapper objectMapper;

    @Override
    public List<Map<String, Object>> getWrongSubjects(Long userId) {
        // 只查询已批改(status>=3)的考试记录，未批改的不显示错题
        List<ExamRecord> records = recordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getUserId, userId).ge(ExamRecord::getStatus, 3));
        if (records.isEmpty()) return Collections.emptyList();
        // 过滤掉成绩未发布的考试记录
        records = filterPublishedRecords(records);
        if (records.isEmpty()) return Collections.emptyList();

        List<Long> recordIds = records.stream().map(ExamRecord::getId).collect(Collectors.toList());
        List<ExamAnswer> wrongAnswers = answerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                .in(ExamAnswer::getRecordId, recordIds)
                .in(ExamAnswer::getIsCorrect, Arrays.asList(0, 2))
                .eq(ExamAnswer::getIsRemoved, 0));

        // 批量预加载题目信息，避免 N+1 查询
        Set<Long> questionIds = wrongAnswers.stream().map(ExamAnswer::getQuestionId).collect(Collectors.toSet());
        Map<Long, ExamQuestion> questionMap = questionIds.isEmpty() ? Collections.emptyMap() :
                questionMapper.selectBatchIds(questionIds).stream()
                        .collect(Collectors.toMap(ExamQuestion::getId, q -> q));

        // 按科目分组统计
        Map<Long, Integer> subjectCountMap = new HashMap<>();
        for (ExamAnswer a : wrongAnswers) {
            ExamQuestion q = questionMap.get(a.getQuestionId());
            if (q != null) {
                subjectCountMap.merge(q.getSubjectId(), 1, Integer::sum);
            }
        }

        // 批量查询科目名称
        Map<Long, String> subjectNameMap = subjectCountMap.keySet().isEmpty() ? Collections.emptyMap() :
                subjectMapper.selectBatchIds(subjectCountMap.keySet()).stream()
                        .collect(Collectors.toMap(EduSubject::getId, EduSubject::getSubjectName));

        return subjectCountMap.entrySet().stream().map(entry -> {
            Map<String, Object> m = new HashMap<>();
            m.put("subjectId", entry.getKey());
            m.put("subjectName", subjectNameMap.getOrDefault(entry.getKey(), ""));
            m.put("wrongCount", entry.getValue());
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getWrongExams(Long userId, Long subjectId) {
        // 查询已批改 + 成绩已发布的考试记录
        List<ExamRecord> records = recordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getUserId, userId).ge(ExamRecord::getStatus, 3));
        if (records.isEmpty()) return Collections.emptyList();
        records = filterPublishedRecords(records);
        if (records.isEmpty()) return Collections.emptyList();

        List<Long> recordIds = records.stream().map(ExamRecord::getId).collect(Collectors.toList());
        LambdaQueryWrapper<ExamAnswer> wrapper = new LambdaQueryWrapper<ExamAnswer>()
                .in(ExamAnswer::getRecordId, recordIds)
                .in(ExamAnswer::getIsCorrect, Arrays.asList(0, 2))
                .eq(ExamAnswer::getIsRemoved, 0);

        // 按科目过滤：先查该科目下的题目ID
        if (subjectId != null) {
            List<Long> questionIds = questionMapper.selectList(
                    new LambdaQueryWrapper<ExamQuestion>().eq(ExamQuestion::getSubjectId, subjectId)
                            .select(ExamQuestion::getId))
                    .stream().map(ExamQuestion::getId).collect(Collectors.toList());
            if (questionIds.isEmpty()) return Collections.emptyList();
            wrapper.in(ExamAnswer::getQuestionId, questionIds);
        }

        List<ExamAnswer> wrongAnswers = answerMapper.selectList(wrapper);
        if (wrongAnswers.isEmpty()) return Collections.emptyList();

        // recordId -> examId
        Map<Long, Long> recordToExamMap = records.stream()
                .collect(Collectors.toMap(ExamRecord::getId, ExamRecord::getExamId));

        // 按 examId 聚合错题数
        Map<Long, Integer> examCountMap = new HashMap<>();
        for (ExamAnswer a : wrongAnswers) {
            Long examId = recordToExamMap.get(a.getRecordId());
            if (examId != null) {
                examCountMap.merge(examId, 1, Integer::sum);
            }
        }
        if (examCountMap.isEmpty()) return Collections.emptyList();

        // 按考试开始时间倒序（最近考试在前）
        List<ExamExam> exams = examMapper.selectBatchIds(examCountMap.keySet());
        return exams.stream()
                .sorted(Comparator.comparing(ExamExam::getStartTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("examId", e.getId());
                    m.put("examName", e.getExamName());
                    m.put("examTime", e.getStartTime() != null ? e.getStartTime().toLocalDate().toString() : "");
                    m.put("wrongCount", examCountMap.get(e.getId()));
                    return m;
                }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getWrongTypeCounts(Long userId, Long subjectId, Long examId) {
        Map<String, Object> result = new HashMap<>();
        result.put("total", 0);
        for (int i = 1; i <= 5; i++) result.put(String.valueOf(i), 0);

        List<ExamRecord> records = recordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getUserId, userId).ge(ExamRecord::getStatus, 3));
        if (records.isEmpty()) return result;
        records = filterPublishedRecords(records);
        if (records.isEmpty()) return result;

        if (examId != null) {
            records = records.stream().filter(r -> examId.equals(r.getExamId())).collect(Collectors.toList());
            if (records.isEmpty()) return result;
        }

        List<Long> recordIds = records.stream().map(ExamRecord::getId).collect(Collectors.toList());
        LambdaQueryWrapper<ExamAnswer> wrapper = new LambdaQueryWrapper<ExamAnswer>()
                .in(ExamAnswer::getRecordId, recordIds)
                .in(ExamAnswer::getIsCorrect, Arrays.asList(0, 2))
                .eq(ExamAnswer::getIsRemoved, 0);

        if (subjectId != null) {
            List<Long> questionIds = questionMapper.selectList(
                    new LambdaQueryWrapper<ExamQuestion>().eq(ExamQuestion::getSubjectId, subjectId)
                            .select(ExamQuestion::getId))
                    .stream().map(ExamQuestion::getId).collect(Collectors.toList());
            if (questionIds.isEmpty()) return result;
            wrapper.in(ExamAnswer::getQuestionId, questionIds);
        }

        List<ExamAnswer> wrongAnswers = answerMapper.selectList(wrapper);
        if (wrongAnswers.isEmpty()) return result;

        // 题目id -> 题型
        Set<Long> qIds = wrongAnswers.stream().map(ExamAnswer::getQuestionId).collect(Collectors.toSet());
        Map<Long, Integer> qTypeMap = questionMapper.selectBatchIds(qIds).stream()
                .filter(q -> q.getQuestionType() != null)
                .collect(Collectors.toMap(ExamQuestion::getId, ExamQuestion::getQuestionType));

        int total = 0;
        int[] counts = new int[6];
        for (ExamAnswer a : wrongAnswers) {
            Integer t = qTypeMap.get(a.getQuestionId());
            if (t != null && t >= 1 && t <= 5) {
                counts[t]++;
                total++;
            }
        }
        result.put("total", total);
        for (int i = 1; i <= 5; i++) result.put(String.valueOf(i), counts[i]);
        return result;
    }

    @Override
    public PageResult<Map<String, Object>> getWrongList(Long userId, Long subjectId, Long examId, Integer questionType, Integer page, Integer size) {
        // 只查询已批改(status>=3)的考试记录，未批改的不显示错题
        List<ExamRecord> records = recordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getUserId, userId).ge(ExamRecord::getStatus, 3));
        if (records.isEmpty()) return new PageResult<>(Collections.emptyList(), 0, page, size);
        // 过滤掉成绩未发布的考试记录
        records = filterPublishedRecords(records);
        if (records.isEmpty()) return new PageResult<>(Collections.emptyList(), 0, page, size);

        // 按考试过滤：仅保留指定考试的记录
        if (examId != null) {
            records = records.stream().filter(r -> examId.equals(r.getExamId())).collect(Collectors.toList());
            if (records.isEmpty()) return new PageResult<>(Collections.emptyList(), 0, page, size);
        }

        List<Long> recordIds = records.stream().map(ExamRecord::getId).collect(Collectors.toList());
        LambdaQueryWrapper<ExamAnswer> wrapper = new LambdaQueryWrapper<ExamAnswer>()
                .in(ExamAnswer::getRecordId, recordIds)
                .in(ExamAnswer::getIsCorrect, Arrays.asList(0, 2))
                .eq(ExamAnswer::getIsRemoved, 0);

        // 按科目和题型过滤：合并为一次题目查询，保证分页准确
        if (subjectId != null || questionType != null) {
            LambdaQueryWrapper<ExamQuestion> qWrapper = new LambdaQueryWrapper<ExamQuestion>()
                    .select(ExamQuestion::getId);
            if (subjectId != null) qWrapper.eq(ExamQuestion::getSubjectId, subjectId);
            if (questionType != null) qWrapper.eq(ExamQuestion::getQuestionType, questionType);
            List<Long> questionIds = questionMapper.selectList(qWrapper)
                    .stream().map(ExamQuestion::getId).collect(Collectors.toList());
            if (questionIds.isEmpty()) return new PageResult<>(Collections.emptyList(), 0, page, size);
            wrapper.in(ExamAnswer::getQuestionId, questionIds);
        }
        wrapper.orderByDesc(ExamAnswer::getCreateTime);

        Page<ExamAnswer> p = answerMapper.selectPage(new Page<>(page, size), wrapper);

        // 批量预加载题目、科目、考试信息，避免 N+1
        Set<Long> questionIdSet = p.getRecords().stream().map(ExamAnswer::getQuestionId).collect(Collectors.toSet());
        Map<Long, ExamQuestion> questionMap = questionIdSet.isEmpty() ? Collections.emptyMap() :
                questionMapper.selectBatchIds(questionIdSet).stream()
                        .collect(Collectors.toMap(ExamQuestion::getId, q -> q));

        Set<Long> subjectIdSet = questionMap.values().stream().map(ExamQuestion::getSubjectId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> subjectNameMap = subjectIdSet.isEmpty() ? Collections.emptyMap() :
                subjectMapper.selectBatchIds(subjectIdSet).stream()
                        .collect(Collectors.toMap(EduSubject::getId, EduSubject::getSubjectName));

        Map<Long, ExamRecord> recordMap = records.stream().collect(Collectors.toMap(ExamRecord::getId, r -> r));
        Set<Long> examIdSet = records.stream().map(ExamRecord::getExamId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, ExamExam> examMap = examIdSet.isEmpty() ? Collections.emptyMap() :
                examMapper.selectBatchIds(examIdSet).stream()
                        .collect(Collectors.toMap(ExamExam::getId, e -> e));

        String[] typeNames = {"", "单选题", "多选题", "判断题", "填空题", "简答题"};

        List<Map<String, Object>> resultList = new ArrayList<>();
        for (ExamAnswer a : p.getRecords()) {
            ExamQuestion q = questionMap.get(a.getQuestionId());
            if (q == null) continue;

            ExamRecord record = recordMap.get(a.getRecordId());
            ExamExam exam = record != null ? examMap.get(record.getExamId()) : null;

            Map<String, Object> m = new HashMap<>();
            m.put("answerId", a.getId());
            m.put("questionId", a.getQuestionId());
            m.put("questionType", q.getQuestionType());
            m.put("questionTypeName", q.getQuestionType() != null && q.getQuestionType() >= 1 && q.getQuestionType() <= 5 ? typeNames[q.getQuestionType()] : "");
            m.put("subjectName", subjectNameMap.getOrDefault(q.getSubjectId(), ""));
            m.put("content", q.getContent());
            m.put("myAnswer", a.getAnswer());
            m.put("correctAnswer", q.getAnswer());
            m.put("examName", exam != null ? exam.getExamName() : "");
            m.put("examTime", exam != null && exam.getStartTime() != null ? exam.getStartTime().toLocalDate().toString() : "");
            resultList.add(m);
        }
        return new PageResult<>(resultList, p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Override
    public Map<String, Object> getWrongDetail(Long answerId, Long userId) {
        ExamAnswer answer = answerMapper.selectById(answerId);
        if (answer == null) throw new BusinessException("错题不存在");

        // 验证归属
        ExamRecord record = recordMapper.selectById(answer.getRecordId());
        if (record == null || !record.getUserId().equals(userId)) throw new BusinessException("无权查看");
        if (record.getStatus() < 3) throw new BusinessException("成绩尚未发布，无法查看错题详情");
        ExamExam exam = examMapper.selectById(record.getExamId());
        if (exam == null || !Integer.valueOf(1).equals(exam.getScorePublished())) {
            throw new BusinessException("成绩尚未发布，无法查看错题详情");
        }

        ExamQuestion q = questionMapper.selectById(answer.getQuestionId());

        // 获取该题在试卷中的分值
        ExamPaperQuestion pq = paperQuestionMapper.selectOne(new LambdaQueryWrapper<ExamPaperQuestion>()
                .eq(ExamPaperQuestion::getPaperId, record.getPaperId())
                .eq(ExamPaperQuestion::getQuestionId, answer.getQuestionId()));

        Map<String, Object> result = new HashMap<>();
        result.put("answerId", answer.getId());
        result.put("questionId", answer.getQuestionId());
        result.put("questionType", q != null ? q.getQuestionType() : 0);
        result.put("content", q != null ? q.getContent() : "");
        result.put("options", q != null ? parseOptions(q.getOptions()) : null);
        result.put("myAnswer", answer.getAnswer());
        result.put("correctAnswer", q != null ? q.getAnswer() : "");
        result.put("analysis", q != null ? q.getAnalysis() : "");
        result.put("score", answer.getScore());
        result.put("fullScore", pq != null ? pq.getScore() : BigDecimal.ZERO);
        result.put("examName", exam != null ? exam.getExamName() : "");
        result.put("examTime", exam != null && exam.getStartTime() != null ? exam.getStartTime().toLocalDate().toString() : "");
        return result;
    }

    @Override
    public void removeWrong(Long answerId, Long userId) {
        ExamAnswer answer = answerMapper.selectById(answerId);
        if (answer == null) throw new BusinessException("错题不存在");

        ExamRecord record = recordMapper.selectById(answer.getRecordId());
        if (record == null || !record.getUserId().equals(userId)) throw new BusinessException("无权操作");
        if (record.getStatus() < 3) throw new BusinessException("成绩尚未发布，无法操作");
        ExamExam examCheck = examMapper.selectById(record.getExamId());
        if (examCheck == null || !Integer.valueOf(1).equals(examCheck.getScorePublished())) {
            throw new BusinessException("成绩尚未发布，无法操作");
        }

        answer.setIsRemoved(1);
        answerMapper.updateById(answer);
    }

    /**
     * 过滤掉成绩未发布的考试记录，只保留对应考试scorePublished=1的记录
     */
    private List<ExamRecord> filterPublishedRecords(List<ExamRecord> records) {
        Set<Long> examIds = records.stream().map(ExamRecord::getExamId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (examIds.isEmpty()) return Collections.emptyList();
        Map<Long, ExamExam> examMap = examMapper.selectBatchIds(examIds).stream()
                .collect(Collectors.toMap(ExamExam::getId, e -> e));
        return records.stream().filter(r -> {
            ExamExam exam = examMap.get(r.getExamId());
            return exam != null && Integer.valueOf(1).equals(exam.getScorePublished());
        }).collect(Collectors.toList());
    }

    private List<String> parseOptions(String json) {
        if (json == null || json.isEmpty()) return null;
        try { return objectMapper.readValue(json, List.class); }
        catch (Exception e) { return null; }
    }
}
