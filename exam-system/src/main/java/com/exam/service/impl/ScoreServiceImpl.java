package com.exam.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.common.constants.RoleConstants;
import com.exam.common.exception.BusinessException;
import com.exam.common.result.PageResult;
import com.exam.entity.*;
import com.exam.mapper.*;
import com.exam.service.ScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScoreServiceImpl implements ScoreService {

    @Autowired private ExamRecordMapper recordMapper;
    @Autowired private ExamExamMapper examMapper;
    @Autowired private ExamPaperMapper paperMapper;
    @Autowired private ExamQuestionMapper questionMapper;
    @Autowired private ExamAnswerMapper answerMapper;
    @Autowired private ExamPaperQuestionMapper paperQuestionMapper;
    @Autowired private SysUserMapper userMapper;
    @Autowired private EduClassMapper classMapper;
    @Autowired private EduSubjectMapper subjectMapper;

    @Override
    public PageResult<Map<String, Object>> getMyScores(Long userId, Integer page, Integer size) {
        // BUG D 修复：先查出已发布成绩的考试ID，在SQL层过滤，确保分页total准确
        List<Long> publishedExamIds = examMapper.selectList(
                new LambdaQueryWrapper<ExamExam>().eq(ExamExam::getScorePublished, 1)
                        .select(ExamExam::getId))
                .stream().map(ExamExam::getId).collect(Collectors.toList());
        if (publishedExamIds.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0, page, size);
        }

        LambdaQueryWrapper<ExamRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamRecord::getUserId, userId).in(ExamRecord::getStatus, 3, 4)
                .in(ExamRecord::getExamId, publishedExamIds)
                .orderByDesc(ExamRecord::getSubmitTime);

        Page<ExamRecord> p = recordMapper.selectPage(new Page<>(page, size), wrapper);
        List<Map<String, Object>> records = new ArrayList<>();

        for (ExamRecord r : p.getRecords()) {
            ExamExam exam = examMapper.selectById(r.getExamId());
            ExamPaper paper = paperMapper.selectById(r.getPaperId());

            Map<String, Object> m = new HashMap<>();
            m.put("recordId", r.getId());
            m.put("examName", exam != null ? exam.getExamName() : "");
            if (paper != null) {
                EduSubject subject = subjectMapper.selectById(paper.getSubjectId());
                m.put("subjectName", subject != null ? subject.getSubjectName() : "");
                m.put("paperTotalScore", paper.getTotalScore());
                m.put("passScore", paper.getPassScore());
                m.put("isPassed", r.getTotalScore() != null && r.getTotalScore().compareTo(paper.getPassScore()) >= 0);
            }
            m.put("totalScore", r.getTotalScore());
            m.put("status", r.getStatus());
            String[] statusNames = {"未开始", "答题中", "已交卷", "已批改", "缺考"};
            m.put("statusName", r.getStatus() != null && r.getStatus() >= 0 && r.getStatus() <= 4 ? statusNames[r.getStatus()] : "");
            m.put("submitTime", r.getSubmitTime());

            if (r.getTotalScore() != null && exam != null) {
                // 排名只统计已批改(3)+缺考(4)，排除待批改(2)的不完整记录
                // 采用竞赛排名（1,1,3），与教师端 getClassScores 保持一致：同分并列，下一名跳过并列数
                long higherCount = recordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getExamId, r.getExamId())
                        .in(ExamRecord::getStatus, 3, 4)
                        .gt(ExamRecord::getTotalScore, r.getTotalScore()));
                long rank = higherCount + 1;
                long totalParticipants = recordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getExamId, r.getExamId())
                        .in(ExamRecord::getStatus, 3, 4));
                m.put("rank", rank);
                m.put("totalParticipants", totalParticipants);
            } else {
                m.put("rank", null);
                m.put("totalParticipants", null);
            }
            records.add(m);
        }
        return new PageResult<>(records, p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Override
    public Map<String, Object> getClassScores(Long examId, Long creatorId) {
        ExamExam exam = examMapper.selectById(examId);
        if (exam == null) throw new BusinessException("考试不存在");
        if (!exam.getCreatorId().equals(creatorId)) throw new BusinessException("无权查看");

        // 计算考试状态
        LocalDateTime now = LocalDateTime.now();
        int examStatus;
        String examStatusName;
        if (now.isBefore(exam.getStartTime())) { examStatus = 0; examStatusName = "未开始"; }
        else if (now.isAfter(exam.getEndTime())) { examStatus = 2; examStatusName = "已结束"; }
        else { examStatus = 1; examStatusName = "进行中"; }
        boolean scorePublished = Integer.valueOf(1).equals(exam.getScorePublished());

        ExamPaper paper = paperMapper.selectById(exam.getPaperId());
        List<ExamRecord> records = recordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, examId).ge(ExamRecord::getStatus, 2)
                .orderByDesc(ExamRecord::getTotalScore));

        String[] statusNames = {"未开始", "答题中", "已交卷", "已批改", "缺考"};

        List<Long> userIds = records.stream().map(ExamRecord::getUserId).distinct().collect(Collectors.toList());
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Collections.emptyMap() :
                userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, u -> u));

        int rank = 0;
        BigDecimal prevScore = null;
        int sameScoreCount = 0;
        List<Map<String, Object>> scoreList = new ArrayList<>();

        for (ExamRecord r : records) {
            BigDecimal currentScore = r.getTotalScore() != null ? r.getTotalScore() : BigDecimal.ZERO;
            if (prevScore == null || currentScore.compareTo(prevScore) != 0) {
                rank = rank + sameScoreCount + (prevScore == null ? 1 : 0);
                if (prevScore != null) rank = scoreList.size() + 1;
                sameScoreCount = 0;
            } else {
                sameScoreCount++;
            }
            prevScore = currentScore;

            SysUser user = userMap.get(r.getUserId());
            Map<String, Object> m = new HashMap<>();
            m.put("recordId", r.getId());
            m.put("rank", scoreList.size() + 1 - sameScoreCount);
            m.put("realName", user != null ? user.getRealName() : "");
            m.put("totalScore", r.getTotalScore());
            m.put("objectiveScore", r.getObjectiveScore());
            m.put("subjectiveScore", r.getSubjectiveScore());
            m.put("isPassed", r.getTotalScore() != null && paper != null &&
                    r.getTotalScore().compareTo(paper.getPassScore()) >= 0);
            m.put("submitTime", r.getSubmitTime() != null ?
                    r.getSubmitTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
            m.put("status", r.getStatus());
            m.put("statusName", r.getStatus() != null && r.getStatus() >= 0 && r.getStatus() <= 4 ? statusNames[r.getStatus()] : "");
            m.put("paperTotalScore", paper != null ? paper.getTotalScore() : BigDecimal.ZERO);
            m.put("passScore", paper != null ? paper.getPassScore() : BigDecimal.ZERO);
            scoreList.add(m);
        }

        // 包装返回：增加考试状态信息
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("examStatus", examStatus);
        wrapper.put("examStatusName", examStatusName);
        wrapper.put("scorePublished", scorePublished);
        wrapper.put("scores", scoreList);
        return wrapper;
    }

    @Override
    public void exportScores(Long examId, Long creatorId, HttpServletResponse response) {
        ExamExam exam = examMapper.selectById(examId);
        if (exam == null) throw new BusinessException("考试不存在");
        if (!exam.getCreatorId().equals(creatorId)) throw new BusinessException("无权操作");

        ExamPaper paper = paperMapper.selectById(exam.getPaperId());
        EduClass eduClass = classMapper.selectById(exam.getClassId());
        List<ExamRecord> records = recordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, examId).ge(ExamRecord::getStatus, 2)
                .orderByDesc(ExamRecord::getTotalScore));

        List<Long> userIds = records.stream().map(ExamRecord::getUserId).distinct().collect(Collectors.toList());
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Collections.emptyMap() :
                userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, u -> u));

        List<List<String>> data = new ArrayList<>();
        int rank = 0;
        BigDecimal prevScore = null;
        int sameCount = 0;
        for (ExamRecord r : records) {
            BigDecimal curScore = r.getTotalScore() != null ? r.getTotalScore() : BigDecimal.ZERO;
            if (prevScore == null || curScore.compareTo(prevScore) != 0) {
                rank = rank + sameCount + (prevScore == null ? 1 : 0);
                if (prevScore != null) rank = data.size() + 1;
                sameCount = 0;
            } else {
                sameCount++;
            }
            prevScore = curScore;
            SysUser user = userMap.get(r.getUserId());
            List<String> row = new ArrayList<>();
            row.add(String.valueOf(data.size() + 1 - sameCount));
            row.add(user != null ? user.getUsername() : "");
            row.add(user != null ? user.getRealName() : "");
            row.add(eduClass != null ? eduClass.getClassName() : "");
            row.add(r.getObjectiveScore() != null ? r.getObjectiveScore().toString() : "0");
            row.add(r.getSubjectiveScore() != null ? r.getSubjectiveScore().toString() : "0");
            row.add(r.getTotalScore() != null ? r.getTotalScore().toString() : "0");
            row.add(r.getTotalScore() != null && paper != null &&
                    r.getTotalScore().compareTo(paper.getPassScore()) >= 0 ? "是" : "否");
            row.add(r.getSubmitTime() != null ? r.getSubmitTime().toString() : "");
            data.add(row);
        }

        try {
            String fileName = exam.getExamName() + "_成绩单_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));

            List<List<String>> head = new ArrayList<>();
            for (String h : new String[]{"排名", "学号", "姓名", "班级", "客观题得分", "主观题得分", "总分", "是否及格", "交卷时间"}) {
                head.add(Collections.singletonList(h));
            }
            EasyExcel.write(response.getOutputStream()).head(head).sheet("成绩单").doWrite(data);
        } catch (Exception e) {
            throw new BusinessException("导出失败：" + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getScoreAnalysis(Long examId, Long creatorId) {
        ExamExam exam = examMapper.selectById(examId);
        if (exam == null) throw new BusinessException("考试不存在");
        if (!exam.getCreatorId().equals(creatorId)) throw new BusinessException("无权查看");

        ExamPaper paper = paperMapper.selectById(exam.getPaperId());
        EduClass eduClass = classMapper.selectById(exam.getClassId());

        long totalStudents = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getClassId, exam.getClassId()).eq(SysUser::getRoleId, RoleConstants.STUDENT_ROLE_ID));
        // BUG E 修复：区分已交卷(status>=2)和已完成(status>=3)记录
        // 统计计算只使用已完成记录，避免待批改(status=2)的null totalScore导致失真
        long submittedCount = recordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, examId).ge(ExamRecord::getStatus, 2));
        List<ExamRecord> records = recordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, examId).in(ExamRecord::getStatus, 3, 4));
        long markedCount = records.stream().filter(r -> r.getStatus() == 3).count();
        long pendingCount = submittedCount - records.size();

        Map<String, Object> result = new HashMap<>();
        result.put("examName", exam.getExamName());
        result.put("className", eduClass != null ? eduClass.getClassName() : "");
        result.put("totalStudents", totalStudents);
        result.put("submittedCount", submittedCount);
        result.put("markedCount", markedCount);
        result.put("pendingCount", pendingCount);

        if (!records.isEmpty()) {
            BigDecimal sum = BigDecimal.ZERO;
            BigDecimal max = BigDecimal.ZERO;
            BigDecimal min = new BigDecimal("999999");
            int passCount = 0;

            for (ExamRecord r : records) {
                BigDecimal score = r.getTotalScore() != null ? r.getTotalScore() : BigDecimal.ZERO;
                sum = sum.add(score);
                if (score.compareTo(max) > 0) max = score;
                if (score.compareTo(min) < 0) min = score;
                if (paper != null && score.compareTo(paper.getPassScore()) >= 0) passCount++;
            }

            result.put("averageScore", sum.divide(new BigDecimal(records.size()), 2, RoundingMode.HALF_UP));
            result.put("maxScore", max);
            result.put("minScore", min.compareTo(new BigDecimal("999999")) == 0 ? BigDecimal.ZERO : min);
            result.put("passRate", new BigDecimal(passCount * 100).divide(new BigDecimal(records.size()), 2, RoundingMode.HALF_UP));
        } else {
            result.put("averageScore", 0);
            result.put("maxScore", 0);
            result.put("minScore", 0);
            result.put("passRate", 0);
        }

        Map<String, Integer> distribution = new LinkedHashMap<>();
        distribution.put("0-59", 0);
        distribution.put("60-69", 0);
        distribution.put("70-79", 0);
        distribution.put("80-89", 0);
        distribution.put("90-100", 0);
        for (ExamRecord r : records) {
            int score = r.getTotalScore() != null ? r.getTotalScore().intValue() : 0;
            if (score < 60) distribution.merge("0-59", 1, Integer::sum);
            else if (score < 70) distribution.merge("60-69", 1, Integer::sum);
            else if (score < 80) distribution.merge("70-79", 1, Integer::sum);
            else if (score < 90) distribution.merge("80-89", 1, Integer::sum);
            else distribution.merge("90-100", 1, Integer::sum);
        }
        result.put("scoreDistribution", distribution);

        List<ExamPaperQuestion> pqs = paperQuestionMapper.selectList(
                new LambdaQueryWrapper<ExamPaperQuestion>().eq(ExamPaperQuestion::getPaperId, exam.getPaperId())
                        .orderByAsc(ExamPaperQuestion::getSortOrder));

        // 题目正确率分析也只使用已完成记录的答案
        Set<Long> recordIds = records.stream().map(ExamRecord::getId).collect(Collectors.toSet());
        List<ExamAnswer> allAnswers = recordIds.isEmpty() ? Collections.emptyList() :
                answerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                        .in(ExamAnswer::getRecordId, recordIds));
        Map<Long, List<ExamAnswer>> answersByQuestion = allAnswers.stream()
                .collect(Collectors.groupingBy(ExamAnswer::getQuestionId));

        List<Map<String, Object>> questionAnalysis = new ArrayList<>();
        for (ExamPaperQuestion pq : pqs) {
            ExamQuestion q = questionMapper.selectById(pq.getQuestionId());
            if (q == null) continue;

            List<ExamAnswer> answers = answersByQuestion.getOrDefault(pq.getQuestionId(), Collections.emptyList());
            long correctCount = answers.stream().filter(a -> a.getIsCorrect() != null && a.getIsCorrect() == 1).count();
            BigDecimal avgScore = BigDecimal.ZERO;
            if (!answers.isEmpty()) {
                BigDecimal totalScore = answers.stream()
                        .map(a -> a.getScore() != null ? a.getScore() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                avgScore = totalScore.divide(new BigDecimal(answers.size()), 2, RoundingMode.HALF_UP);
            }

            Map<String, Object> qa = new HashMap<>();
            qa.put("questionId", q.getId());
            qa.put("content", q.getContent());
            qa.put("questionType", q.getQuestionType());
            qa.put("correctRate", answers.isEmpty() ? 0 :
                    new BigDecimal(correctCount * 100).divide(new BigDecimal(answers.size()), 2, RoundingMode.HALF_UP));
            qa.put("averageScore", avgScore);
            qa.put("fullScore", pq.getScore());
            questionAnalysis.add(qa);
        }
        result.put("questionAnalysis", questionAnalysis);
        return result;
    }
}
