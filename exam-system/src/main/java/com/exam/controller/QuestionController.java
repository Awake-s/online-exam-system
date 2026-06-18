package com.exam.controller;

import com.exam.common.result.PageResult;
import com.exam.common.result.Result;
import com.exam.dto.request.QuestionAddRequest;
import com.exam.security.SecurityUtils;
import com.exam.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/question")
@PreAuthorize("hasRole('TEACHER')")
public class QuestionController {

    @Autowired
    private QuestionService questionService;

    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Integer questionType,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) String keyword) {
        return Result.success(questionService.listQuestions(page, size, subjectId, questionType, difficulty, keyword, SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.success(questionService.getQuestionDetail(id, SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/add")
    public Result<Void> add(@Valid @RequestBody QuestionAddRequest request) {
        questionService.addQuestion(request, SecurityUtils.getCurrentUserId());
        return Result.success("添加成功", null);
    }

    @PutMapping("/update/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody QuestionAddRequest request) {
        questionService.updateQuestion(id, request, SecurityUtils.getCurrentUserId());
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        String msg = questionService.deleteQuestion(id, SecurityUtils.getCurrentUserId());
        return Result.success(msg, msg);
    }

    @DeleteMapping("/batch")
    public Result<Map<String, Object>> batchDelete(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error(400, "请选择要删除的题目");
        }
        return Result.success(questionService.batchDeleteQuestions(ids, SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/import")
    public Result<Map<String, Object>> importQuestions(@RequestParam("file") MultipartFile file, @RequestParam Long subjectId) {
        return Result.success(questionService.importQuestions(file, subjectId, SecurityUtils.getCurrentUserId()));
    }
}
