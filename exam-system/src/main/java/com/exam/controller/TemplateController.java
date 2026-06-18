package com.exam.controller;

import com.exam.common.result.Result;
import com.exam.dto.request.TemplateCreateRequest;
import com.exam.security.SecurityUtils;
import com.exam.service.TemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/template")
@PreAuthorize("hasRole('TEACHER')")
public class TemplateController {

    @Autowired
    private TemplateService templateService;

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(
            @RequestParam(required = false) Long subjectId) {
        return Result.success(templateService.listTemplates(subjectId, SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.success(templateService.getTemplateDetail(id, SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/add")
    public Result<Void> add(@Valid @RequestBody TemplateCreateRequest request) {
        templateService.createTemplate(request, SecurityUtils.getCurrentUserId());
        return Result.success("创建成功", null);
    }

    @PutMapping("/update/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody TemplateCreateRequest request) {
        templateService.updateTemplate(id, request, SecurityUtils.getCurrentUserId());
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        templateService.deleteTemplate(id, SecurityUtils.getCurrentUserId());
        return Result.success("删除成功", null);
    }
}
