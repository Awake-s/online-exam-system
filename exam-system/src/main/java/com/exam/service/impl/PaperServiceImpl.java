package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.common.exception.BusinessException;
import com.exam.common.result.PageResult;
import com.exam.dto.request.PaperCreateRequest;
import com.exam.dto.request.PaperRandomRequest;
import com.exam.entity.*;
import com.exam.mapper.*;
import com.exam.service.PaperService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PaperServiceImpl implements PaperService {

    @Autowired private ExamPaperMapper paperMapper;
    @Autowired private ExamPaperQuestionMapper paperQuestionMapper;
    @Autowired private ExamQuestionMapper questionMapper;
    @Autowired private EduSubjectMapper subjectMapper;
    @Autowired private ExamExamMapper examMapper;
    @Autowired private ObjectMapper objectMapper;

    @Override
    public PageResult<Map<String, Object>> listPapers(Integer page, Integer size, Long subjectId, String paperName, Integer status, Long creatorId) {
        LambdaQueryWrapper<ExamPaper> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamPaper::getCreatorId, creatorId);
        if (subjectId != null) wrapper.eq(ExamPaper::getSubjectId, subjectId);
        if (StringUtils.hasText(paperName)) wrapper.like(ExamPaper::getPaperName, paperName);
        if (status != null) wrapper.eq(ExamPaper::getStatus, status);
        wrapper.orderByDesc(ExamPaper::getCreateTime);

        Page<ExamPaper> p = paperMapper.selectPage(new Page<>(page, size), wrapper);
        Map<Long, String> subjectMap = subjectMapper.selectList(null).stream()
                .collect(Collectors.toMap(EduSubject::getId, EduSubject::getSubjectName));

        List<Map<String, Object>> records = p.getRecords().stream().map(paper -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", paper.getId());
            m.put("paperName", paper.getPaperName());
            m.put("subjectId", paper.getSubjectId());
            m.put("subjectName", subjectMap.getOrDefault(paper.getSubjectId(), ""));
            m.put("totalScore", paper.getTotalScore());
            m.put("passScore", paper.getPassScore());
            m.put("duration", paper.getDuration());
            long qCount = paperQuestionMapper.selectCount(new LambdaQueryWrapper<ExamPaperQuestion>()
                    .eq(ExamPaperQuestion::getPaperId, paper.getId()));
            m.put("questionCount", qCount);
            m.put("status", paper.getStatus());
            long examCount = examMapper.selectCount(new LambdaQueryWrapper<ExamExam>().eq(ExamExam::getPaperId, paper.getId()));
            m.put("examCount", examCount);
            String statusName;
            if (paper.getStatus() == 0) {
                statusName = "草稿";
            } else if (examCount > 0) {
                statusName = "已使用";
            } else {
                statusName = "可使用";
            }
            m.put("statusName", statusName);
            m.put("createTime", paper.getCreateTime());
            return m;
        }).collect(Collectors.toList());
        return new PageResult<>(records, p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Override
    public Map<String, Object> getPaperDetail(Long id, Long creatorId) {
        ExamPaper paper = paperMapper.selectById(id);
        if (paper == null) throw new BusinessException("试卷不存在");
        if (!paper.getCreatorId().equals(creatorId)) throw new BusinessException("只能查看自己创建的试卷");

        Map<String, Object> result = new HashMap<>();
        result.put("id", paper.getId());
        result.put("paperName", paper.getPaperName());
        result.put("subjectId", paper.getSubjectId());
        EduSubject subject = subjectMapper.selectById(paper.getSubjectId());
        result.put("subjectName", subject != null ? subject.getSubjectName() : "");
        result.put("totalScore", paper.getTotalScore());
        result.put("passScore", paper.getPassScore());
        result.put("duration", paper.getDuration());
        result.put("status", paper.getStatus());

        List<ExamPaperQuestion> pqs = paperQuestionMapper.selectList(
                new LambdaQueryWrapper<ExamPaperQuestion>()
                        .eq(ExamPaperQuestion::getPaperId, id)
                        .orderByAsc(ExamPaperQuestion::getSortOrder));

        List<Map<String, Object>> questions = new ArrayList<>();
        String[] typeNames = {"", "单选题", "多选题", "判断题", "填空题", "简答题"};
        for (ExamPaperQuestion pq : pqs) {
            ExamQuestion q = questionMapper.selectById(pq.getQuestionId());
            if (q == null) continue;
            Map<String, Object> qm = new HashMap<>();
            qm.put("id", q.getId());
            qm.put("paperQuestionId", pq.getId());
            qm.put("questionType", q.getQuestionType());
            qm.put("questionTypeName", q.getQuestionType() != null && q.getQuestionType() >= 1 && q.getQuestionType() <= 5 ? typeNames[q.getQuestionType()] : "");
            qm.put("content", q.getContent());
            qm.put("options", parseOptions(q.getOptions()));
            qm.put("answer", q.getAnswer());
            qm.put("analysis", q.getAnalysis());
            qm.put("score", pq.getScore());
            qm.put("sortOrder", pq.getSortOrder());
            questions.add(qm);
        }
        result.put("questions", questions);
        return result;
    }

    @Override
    @Transactional
    public void createPaper(PaperCreateRequest request, Long creatorId) {
        BigDecimal totalScore = request.getQuestions().stream()
                .map(PaperCreateRequest.PaperQuestionItem::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ExamPaper paper = new ExamPaper();
        paper.setPaperName(request.getPaperName());
        paper.setSubjectId(request.getSubjectId());
        paper.setTotalScore(totalScore);
        paper.setPassScore(request.getPassScore());
        paper.setDuration(request.getDuration());
        paper.setCreatorId(creatorId);
        paper.setStatus(0);
        paperMapper.insert(paper);

        for (PaperCreateRequest.PaperQuestionItem item : request.getQuestions()) {
            ExamPaperQuestion pq = new ExamPaperQuestion();
            pq.setPaperId(paper.getId());
            pq.setQuestionId(item.getQuestionId());
            pq.setScore(item.getScore());
            pq.setSortOrder(item.getSortOrder());
            paperQuestionMapper.insert(pq);
        }
    }

    @Override
    @Transactional
    public void updatePaper(Long id, PaperCreateRequest request, Long creatorId) {
        ExamPaper paper = paperMapper.selectById(id);
        if (paper == null) throw new BusinessException("试卷不存在");
        if (!paper.getCreatorId().equals(creatorId)) throw new BusinessException("只能编辑自己创建的试卷");

        long examCount = examMapper.selectCount(new LambdaQueryWrapper<ExamExam>().eq(ExamExam::getPaperId, id));
        if (examCount > 0) throw new BusinessException("该试卷已被考试使用，无法编辑");

        BigDecimal totalScore = request.getQuestions().stream()
                .map(PaperCreateRequest.PaperQuestionItem::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        paper.setPaperName(request.getPaperName());
        paper.setSubjectId(request.getSubjectId());
        paper.setTotalScore(totalScore);
        paper.setPassScore(request.getPassScore());
        paper.setDuration(request.getDuration());
        paperMapper.updateById(paper);

        // 删除旧题目关联，重新插入
        paperQuestionMapper.delete(new LambdaQueryWrapper<ExamPaperQuestion>().eq(ExamPaperQuestion::getPaperId, id));
        for (PaperCreateRequest.PaperQuestionItem item : request.getQuestions()) {
            ExamPaperQuestion pq = new ExamPaperQuestion();
            pq.setPaperId(id);
            pq.setQuestionId(item.getQuestionId());
            pq.setScore(item.getScore());
            pq.setSortOrder(item.getSortOrder());
            paperQuestionMapper.insert(pq);
        }
    }

    @Override
    @Transactional
    public void randomPaper(PaperRandomRequest request, Long creatorId) {
        BigDecimal totalScore = BigDecimal.ZERO;
        List<ExamPaperQuestion> allPqs = new ArrayList<>();
        int sortOrder = 1;

        for (PaperRandomRequest.RandomRule rule : request.getRules()) {
            LambdaQueryWrapper<ExamQuestion> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ExamQuestion::getSubjectId, request.getSubjectId());
            wrapper.eq(ExamQuestion::getCreatorId, creatorId);
            wrapper.eq(ExamQuestion::getDeleted, 0);
            wrapper.eq(ExamQuestion::getQuestionType, rule.getQuestionType());
            if (rule.getDifficulty() != null) wrapper.eq(ExamQuestion::getDifficulty, rule.getDifficulty());

            List<ExamQuestion> pool = questionMapper.selectList(wrapper);
            String[] typeNames = {"", "单选题", "多选题", "判断题", "填空题", "简答题"};
            String typeName = rule.getQuestionType() != null && rule.getQuestionType() >= 1 && rule.getQuestionType() <= 5 ? typeNames[rule.getQuestionType()] : "题目";

            if (pool.size() < rule.getCount()) {
                throw new BusinessException(typeName + "题库不足，需要" + rule.getCount() + "道，当前仅有" + pool.size() + "道");
            }

            Collections.shuffle(pool);
            List<ExamQuestion> selected = pool.subList(0, rule.getCount());
            for (ExamQuestion q : selected) {
                ExamPaperQuestion pq = new ExamPaperQuestion();
                pq.setQuestionId(q.getId());
                pq.setScore(rule.getScorePerQuestion());
                pq.setSortOrder(sortOrder++);
                allPqs.add(pq);
                totalScore = totalScore.add(rule.getScorePerQuestion());
            }
        }

        ExamPaper paper = new ExamPaper();
        paper.setPaperName(request.getPaperName());
        paper.setSubjectId(request.getSubjectId());
        paper.setTotalScore(totalScore);
        paper.setPassScore(request.getPassScore());
        paper.setDuration(request.getDuration());
        paper.setCreatorId(creatorId);
        paper.setStatus(0);
        paperMapper.insert(paper);

        for (ExamPaperQuestion pq : allPqs) {
            pq.setPaperId(paper.getId());
            paperQuestionMapper.insert(pq);
        }
    }

    @Override
    @Transactional
    public void deletePaper(Long id, Long creatorId) {
        ExamPaper paper = paperMapper.selectById(id);
        if (paper == null) throw new BusinessException("试卷不存在");
        if (!paper.getCreatorId().equals(creatorId)) throw new BusinessException("只能删除自己创建的试卷");

        long examCount = examMapper.selectCount(new LambdaQueryWrapper<ExamExam>().eq(ExamExam::getPaperId, id));
        if (examCount > 0) throw new BusinessException("该试卷已被考试使用，无法删除");

        paperQuestionMapper.delete(new LambdaQueryWrapper<ExamPaperQuestion>().eq(ExamPaperQuestion::getPaperId, id));
        paperMapper.deleteById(id);
    }

    @Override
    public void togglePublish(Long id, Long creatorId) {
        ExamPaper paper = paperMapper.selectById(id);
        if (paper == null) throw new BusinessException("试卷不存在");
        if (!paper.getCreatorId().equals(creatorId)) throw new BusinessException("只能操作自己创建的试卷");

        if (paper.getStatus() == 1) {
            // 撤销发布前检查是否已被考试引用
            long examCount = examMapper.selectCount(new LambdaQueryWrapper<ExamExam>().eq(ExamExam::getPaperId, id));
            if (examCount > 0) throw new BusinessException("该试卷已被考试使用，无法停用");
            paper.setStatus(0);
        } else {
            paper.setStatus(1);
        }
        paperMapper.updateById(paper);
    }

    private List<String> parseOptions(String json) {
        if (json == null || json.isEmpty()) return null;
        try { return objectMapper.readValue(json, List.class); }
        catch (Exception e) { return null; }
    }
}
