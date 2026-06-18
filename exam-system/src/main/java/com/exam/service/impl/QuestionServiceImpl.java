package com.exam.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.common.exception.BusinessException;
import com.exam.common.result.PageResult;
import com.exam.dto.request.QuestionAddRequest;
import com.exam.entity.EduSubject;
import com.exam.entity.ExamPaperQuestion;
import com.exam.entity.ExamQuestion;
import com.exam.mapper.EduSubjectMapper;
import com.exam.mapper.ExamPaperQuestionMapper;
import com.exam.mapper.ExamQuestionMapper;
import com.exam.service.QuestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class QuestionServiceImpl implements QuestionService {

    @Autowired private ExamQuestionMapper questionMapper;
    @Autowired private EduSubjectMapper subjectMapper;
    @Autowired private ExamPaperQuestionMapper paperQuestionMapper;
    @Autowired private ObjectMapper objectMapper;

    private static final String[] TYPE_NAMES = {"", "单选题", "多选题", "判断题", "填空题", "简答题"};
    private static final String[] DIFF_NAMES = {"", "简单", "中等", "困难"};

    @Override
    public PageResult<Map<String, Object>> listQuestions(Integer page, Integer size, Long subjectId, Integer questionType, Integer difficulty, String keyword, Long creatorId) {
        LambdaQueryWrapper<ExamQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamQuestion::getCreatorId, creatorId);
        wrapper.eq(ExamQuestion::getDeleted, 0);
        if (subjectId != null) wrapper.eq(ExamQuestion::getSubjectId, subjectId);
        if (questionType != null) wrapper.eq(ExamQuestion::getQuestionType, questionType);
        if (difficulty != null) wrapper.eq(ExamQuestion::getDifficulty, difficulty);
        if (StringUtils.hasText(keyword)) wrapper.like(ExamQuestion::getContent, keyword);
        wrapper.orderByDesc(ExamQuestion::getCreateTime);

        Page<ExamQuestion> p = questionMapper.selectPage(new Page<>(page, size), wrapper);
        Map<Long, String> subjectMap = subjectMapper.selectList(null).stream()
                .collect(Collectors.toMap(EduSubject::getId, EduSubject::getSubjectName));

        List<Map<String, Object>> records = p.getRecords().stream().map(q -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", q.getId());
            m.put("subjectId", q.getSubjectId());
            m.put("subjectName", subjectMap.getOrDefault(q.getSubjectId(), ""));
            m.put("questionType", q.getQuestionType());
            m.put("questionTypeName", q.getQuestionType() != null && q.getQuestionType() >= 1 && q.getQuestionType() <= 5 ? TYPE_NAMES[q.getQuestionType()] : "");
            m.put("content", q.getContent());
            m.put("options", parseOptions(q.getOptions()));
            m.put("answer", q.getAnswer());
            m.put("analysis", q.getAnalysis());
            m.put("score", q.getScore());
            m.put("difficulty", q.getDifficulty());
            m.put("difficultyName", q.getDifficulty() != null && q.getDifficulty() >= 1 && q.getDifficulty() <= 3 ? DIFF_NAMES[q.getDifficulty()] : "");
            m.put("createTime", q.getCreateTime());
            return m;
        }).collect(Collectors.toList());
        return new PageResult<>(records, p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Override
    public void addQuestion(QuestionAddRequest request, Long creatorId) {
        ExamQuestion q = new ExamQuestion();
        q.setSubjectId(request.getSubjectId());
        q.setQuestionType(request.getQuestionType());
        q.setContent(request.getContent());
        q.setOptions(toJsonString(request.getOptions()));
        q.setAnswer(request.getAnswer());
        q.setAnalysis(request.getAnalysis());
        q.setScore(request.getScore());
        q.setDifficulty(request.getDifficulty());
        q.setCreatorId(creatorId);
        questionMapper.insert(q);
    }

    @Override
    public void updateQuestion(Long id, QuestionAddRequest request, Long creatorId) {
        ExamQuestion q = questionMapper.selectById(id);
        if (q == null) throw new BusinessException("题目不存在");
        if (!q.getCreatorId().equals(creatorId)) throw new BusinessException("只能修改自己创建的题目");

        q.setSubjectId(request.getSubjectId());
        q.setQuestionType(request.getQuestionType());
        q.setContent(request.getContent());
        q.setOptions(toJsonString(request.getOptions()));
        q.setAnswer(request.getAnswer());
        q.setAnalysis(request.getAnalysis());
        q.setScore(request.getScore());
        q.setDifficulty(request.getDifficulty());
        questionMapper.updateById(q);
    }

    @Override
    public String deleteQuestion(Long id, Long creatorId) {
        ExamQuestion q = questionMapper.selectById(id);
        if (q == null) throw new BusinessException("题目不存在");
        if (!q.getCreatorId().equals(creatorId)) throw new BusinessException("只能删除自己创建的题目");

        long usedCount = paperQuestionMapper.selectCount(new LambdaQueryWrapper<ExamPaperQuestion>()
                .eq(ExamPaperQuestion::getQuestionId, id));
        if (usedCount > 0) {
            q.setDeleted(1);
            questionMapper.updateById(q);
            return "该题目已被试卷引用，已从题库中移除（试卷中仍保留）";
        }
        questionMapper.deleteById(id);
        return "删除成功";
    }

    @Override
    @Transactional
    public Map<String, Object> batchDeleteQuestions(List<Long> ids, Long creatorId) {
        int successCount = 0;
        int softDeleteCount = 0;
        int skipCount = 0;
        for (Long id : ids) {
            ExamQuestion q = questionMapper.selectById(id);
            if (q == null || !q.getCreatorId().equals(creatorId)) {
                skipCount++;
                continue;
            }
            long usedCount = paperQuestionMapper.selectCount(new LambdaQueryWrapper<ExamPaperQuestion>()
                    .eq(ExamPaperQuestion::getQuestionId, id));
            if (usedCount > 0) {
                q.setDeleted(1);
                questionMapper.updateById(q);
                softDeleteCount++;
            } else {
                questionMapper.deleteById(id);
                successCount++;
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("success", successCount);
        result.put("softDelete", softDeleteCount);
        result.put("skip", skipCount);
        result.put("total", ids.size());
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> importQuestions(MultipartFile file, Long subjectId, Long creatorId) {
        List<Map<String, Object>> failDetails = new ArrayList<>();
        List<ExamQuestion> successList = new ArrayList<>();

        try {
            EasyExcel.read(file.getInputStream(), new ReadListener<Map<Integer, String>>() {
                int rowNum = 1;
                @Override
                public void invoke(Map<Integer, String> data, AnalysisContext context) {
                    rowNum++;
                    try {
                        ExamQuestion q = parseExcelRow(data, subjectId, creatorId);
                        successList.add(q);
                    } catch (Exception e) {
                        Map<String, Object> fail = new HashMap<>();
                        fail.put("row", rowNum);
                        fail.put("reason", e.getMessage());
                        failDetails.add(fail);
                    }
                }
                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {}
            }).sheet().doRead();
        } catch (Exception e) {
            throw new BusinessException("Excel文件读取失败：" + e.getMessage());
        }

        // 批量插入成功的题目
        for (ExamQuestion q : successList) {
            questionMapper.insert(q);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", successList.size() + failDetails.size());
        result.put("success", successList.size());
        result.put("fail", failDetails.size());
        result.put("failDetails", failDetails);
        return result;
    }

    private ExamQuestion parseExcelRow(Map<Integer, String> data, Long subjectId, Long creatorId) {
        String typeName = data.get(0);
        if (!StringUtils.hasText(typeName)) throw new RuntimeException("题型不能为空");
        int type;
        switch (typeName.trim()) {
            case "单选": type = 1; break;
            case "多选": type = 2; break;
            case "判断": type = 3; break;
            case "填空": type = 4; break;
            case "简答": type = 5; break;
            default: throw new RuntimeException("题型格式错误");
        }

        String content = data.get(1);
        if (!StringUtils.hasText(content)) throw new RuntimeException("题目内容不能为空");

        List<String> options = new ArrayList<>();
        if (type == 1 || type == 2) {
            for (int i = 2; i <= 5; i++) {
                if (StringUtils.hasText(data.get(i))) {
                    options.add(String.valueOf((char)('A' + i - 2)) + "." + data.get(i));
                }
            }
        }

        String answer = data.get(6);
        if (!StringUtils.hasText(answer)) throw new RuntimeException("答案不能为空");
        if (type == 3) {
            answer = "正确".equals(answer.trim()) ? "1" : "0";
        }

        String analysis = data.get(7);
        String scoreStr = data.get(8);
        if (!StringUtils.hasText(scoreStr)) throw new RuntimeException("分值不能为空");

        String diffName = data.get(9);
        int diff;
        String diffVal = diffName != null ? diffName.trim() : "中等";
        switch (diffVal) {
            case "简单": diff = 1; break;
            case "困难": diff = 3; break;
            default: diff = 2; break;
        }

        ExamQuestion q = new ExamQuestion();
        q.setSubjectId(subjectId);
        q.setQuestionType(type);
        q.setContent(content);
        q.setOptions(options.isEmpty() ? null : toJsonString(options));
        q.setAnswer(answer);
        q.setAnalysis(analysis);
        q.setScore(new java.math.BigDecimal(scoreStr.trim()));
        q.setDifficulty(diff);
        q.setCreatorId(creatorId);
        return q;
    }

    @Override
    public Map<String, Object> getQuestionDetail(Long id, Long creatorId) {
        ExamQuestion q = questionMapper.selectById(id);
        if (q == null) throw new BusinessException("题目不存在");
        if (!q.getCreatorId().equals(creatorId)) throw new BusinessException("只能查看自己创建的题目");

        Map<String, Object> m = new HashMap<>();
        m.put("id", q.getId());
        m.put("subjectId", q.getSubjectId());
        EduSubject subject = subjectMapper.selectById(q.getSubjectId());
        m.put("subjectName", subject != null ? subject.getSubjectName() : "");
        m.put("questionType", q.getQuestionType());
        m.put("questionTypeName", q.getQuestionType() != null && q.getQuestionType() >= 1 && q.getQuestionType() <= 5 ? TYPE_NAMES[q.getQuestionType()] : "");
        m.put("content", q.getContent());
        m.put("options", parseOptions(q.getOptions()));
        m.put("answer", q.getAnswer());
        m.put("analysis", q.getAnalysis());
        m.put("score", q.getScore());
        m.put("difficulty", q.getDifficulty());
        m.put("difficultyName", q.getDifficulty() != null && q.getDifficulty() >= 1 && q.getDifficulty() <= 3 ? DIFF_NAMES[q.getDifficulty()] : "");
        m.put("createTime", q.getCreateTime());
        return m;
    }

    private List<String> parseOptions(String json) {
        if (!StringUtils.hasText(json)) return null;
        try { return objectMapper.readValue(json, List.class); }
        catch (Exception e) { return null; }
    }

    private String toJsonString(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        try { return objectMapper.writeValueAsString(list); }
        catch (Exception e) { return null; }
    }
}
