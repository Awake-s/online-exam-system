package com.exam.service;

import com.exam.dto.request.TemplateCreateRequest;
import java.util.List;
import java.util.Map;

public interface TemplateService {
    List<Map<String, Object>> listTemplates(Long subjectId, Long creatorId);
    Map<String, Object> getTemplateDetail(Long id, Long creatorId);
    void createTemplate(TemplateCreateRequest request, Long creatorId);
    void updateTemplate(Long id, TemplateCreateRequest request, Long creatorId);
    void deleteTemplate(Long id, Long creatorId);
}
