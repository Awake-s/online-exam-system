package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.common.exception.BusinessException;
import com.exam.dto.request.TemplateCreateRequest;
import com.exam.entity.EduSubject;
import com.exam.entity.ExamPaperTemplate;
import com.exam.entity.ExamTemplateRule;
import com.exam.mapper.EduSubjectMapper;
import com.exam.mapper.ExamPaperTemplateMapper;
import com.exam.mapper.ExamTemplateRuleMapper;
import com.exam.service.TemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TemplateServiceImpl implements TemplateService {

    @Autowired private ExamPaperTemplateMapper templateMapper;
    @Autowired private ExamTemplateRuleMapper ruleMapper;
    @Autowired private EduSubjectMapper subjectMapper;

    private static final String[] TYPE_NAMES = {"", "单选题", "多选题", "判断题", "填空题", "简答题"};
    private static final String[] DIFF_NAMES = {"", "简单", "中等", "困难"};

    @Override
    public List<Map<String, Object>> listTemplates(Long subjectId, Long creatorId) {
        LambdaQueryWrapper<ExamPaperTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamPaperTemplate::getCreatorId, creatorId);
        if (subjectId != null) wrapper.eq(ExamPaperTemplate::getSubjectId, subjectId);
        wrapper.orderByDesc(ExamPaperTemplate::getUpdateTime);

        List<ExamPaperTemplate> templates = templateMapper.selectList(wrapper);
        Map<Long, String> subjectMap = subjectMapper.selectList(null).stream()
                .collect(Collectors.toMap(EduSubject::getId, EduSubject::getSubjectName));

        return templates.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("templateName", t.getTemplateName());
            m.put("subjectId", t.getSubjectId());
            m.put("subjectName", subjectMap.getOrDefault(t.getSubjectId(), ""));
            m.put("targetScore", t.getTargetScore());
            m.put("passScore", t.getPassScore());
            m.put("duration", t.getDuration());
            m.put("description", t.getDescription());
            m.put("createTime", t.getCreateTime());
            m.put("updateTime", t.getUpdateTime());

            // 查询规则并计算统计信息
            List<ExamTemplateRule> rules = ruleMapper.selectList(
                    new LambdaQueryWrapper<ExamTemplateRule>()
                            .eq(ExamTemplateRule::getTemplateId, t.getId())
                            .orderByAsc(ExamTemplateRule::getSortOrder));

            int totalCount = rules.stream().mapToInt(ExamTemplateRule::getQuestionCount).sum();
            BigDecimal totalScore = rules.stream()
                    .map(r -> r.getScorePerQuestion().multiply(new BigDecimal(r.getQuestionCount())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            m.put("questionCount", totalCount);
            m.put("totalScore", totalScore);
            m.put("ruleCount", rules.size());

            // 题型摘要
            String typeSummary = rules.stream()
                    .map(r -> {
                        String name = (r.getQuestionType() >= 1 && r.getQuestionType() <= 5) ? TYPE_NAMES[r.getQuestionType()] : "题型" + r.getQuestionType();
                        return name + "×" + r.getQuestionCount();
                    })
                    .collect(Collectors.joining("、"));
            m.put("typeSummary", typeSummary);

            // 题型详情列表（供前端可视化）
            List<Map<String, Object>> ruleDetails = rules.stream().map(r -> {
                Map<String, Object> rd = new LinkedHashMap<>();
                rd.put("type", r.getQuestionType());
                rd.put("count", r.getQuestionCount());
                rd.put("score", r.getScorePerQuestion().multiply(new BigDecimal(r.getQuestionCount())));
                return rd;
            }).collect(Collectors.toList());
            m.put("ruleDetails", ruleDetails);

            return m;
        }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getTemplateDetail(Long id, Long creatorId) {
        ExamPaperTemplate template = templateMapper.selectById(id);
        if (template == null) throw new BusinessException("模板不存在");
        if (!template.getCreatorId().equals(creatorId)) throw new BusinessException("只能查看自己创建的模板");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", template.getId());
        result.put("templateName", template.getTemplateName());
        result.put("subjectId", template.getSubjectId());
        EduSubject subject = subjectMapper.selectById(template.getSubjectId());
        result.put("subjectName", subject != null ? subject.getSubjectName() : "");
        result.put("targetScore", template.getTargetScore());
        result.put("passScore", template.getPassScore());
        result.put("duration", template.getDuration());
        result.put("description", template.getDescription());

        List<ExamTemplateRule> rules = ruleMapper.selectList(
                new LambdaQueryWrapper<ExamTemplateRule>()
                        .eq(ExamTemplateRule::getTemplateId, id)
                        .orderByAsc(ExamTemplateRule::getSortOrder));

        List<Map<String, Object>> ruleList = rules.stream().map(r -> {
            Map<String, Object> rm = new LinkedHashMap<>();
            rm.put("id", r.getId());
            rm.put("questionType", r.getQuestionType());
            rm.put("questionTypeName", (r.getQuestionType() >= 1 && r.getQuestionType() <= 5) ? TYPE_NAMES[r.getQuestionType()] : "");
            rm.put("questionCount", r.getQuestionCount());
            rm.put("scorePerQuestion", r.getScorePerQuestion());
            rm.put("difficulty", r.getDifficulty());
            rm.put("difficultyName", r.getDifficulty() != null && r.getDifficulty() >= 1 && r.getDifficulty() <= 3 ? DIFF_NAMES[r.getDifficulty()] : "不限");
            rm.put("sortOrder", r.getSortOrder());
            return rm;
        }).collect(Collectors.toList());

        result.put("rules", ruleList);
        return result;
    }

    @Override
    @Transactional
    public void createTemplate(TemplateCreateRequest request, Long creatorId) {
        ExamPaperTemplate template = new ExamPaperTemplate();
        template.setTemplateName(request.getTemplateName());
        template.setSubjectId(request.getSubjectId());
        template.setTargetScore(request.getTargetScore() != null ? request.getTargetScore() : new BigDecimal("100"));
        template.setPassScore(request.getPassScore() != null ? request.getPassScore() : new BigDecimal("60"));
        template.setDuration(request.getDuration() != null ? request.getDuration() : 120);
        template.setDescription(request.getDescription());
        template.setCreatorId(creatorId);
        templateMapper.insert(template);

        saveRules(template.getId(), request.getRules());
    }

    @Override
    @Transactional
    public void updateTemplate(Long id, TemplateCreateRequest request, Long creatorId) {
        ExamPaperTemplate template = templateMapper.selectById(id);
        if (template == null) throw new BusinessException("模板不存在");
        if (!template.getCreatorId().equals(creatorId)) throw new BusinessException("只能编辑自己创建的模板");

        template.setTemplateName(request.getTemplateName());
        template.setSubjectId(request.getSubjectId());
        if (request.getTargetScore() != null) template.setTargetScore(request.getTargetScore());
        if (request.getPassScore() != null) template.setPassScore(request.getPassScore());
        if (request.getDuration() != null) template.setDuration(request.getDuration());
        template.setDescription(request.getDescription());
        templateMapper.updateById(template);

        // 删除旧规则，重新插入
        ruleMapper.delete(new LambdaQueryWrapper<ExamTemplateRule>()
                .eq(ExamTemplateRule::getTemplateId, id));
        saveRules(id, request.getRules());
    }

    @Override
    @Transactional
    public void deleteTemplate(Long id, Long creatorId) {
        ExamPaperTemplate template = templateMapper.selectById(id);
        if (template == null) throw new BusinessException("模板不存在");
        if (!template.getCreatorId().equals(creatorId)) throw new BusinessException("只能删除自己创建的模板");

        ruleMapper.delete(new LambdaQueryWrapper<ExamTemplateRule>()
                .eq(ExamTemplateRule::getTemplateId, id));
        templateMapper.deleteById(id);
    }

    private void saveRules(Long templateId, List<TemplateCreateRequest.RuleItem> rules) {
        for (int i = 0; i < rules.size(); i++) {
            TemplateCreateRequest.RuleItem item = rules.get(i);
            ExamTemplateRule rule = new ExamTemplateRule();
            rule.setTemplateId(templateId);
            rule.setQuestionType(item.getQuestionType());
            rule.setQuestionCount(item.getQuestionCount());
            rule.setScorePerQuestion(item.getScorePerQuestion());
            rule.setDifficulty(item.getDifficulty());
            rule.setSortOrder(item.getSortOrder() != null ? item.getSortOrder() : i + 1);
            ruleMapper.insert(rule);
        }
    }
}
