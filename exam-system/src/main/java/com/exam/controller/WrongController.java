package com.exam.controller;

import com.exam.common.result.PageResult;
import com.exam.common.result.Result;
import com.exam.security.SecurityUtils;
import com.exam.service.WrongService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wrong")
@PreAuthorize("hasRole('STUDENT')")
public class WrongController {

    @Autowired
    private WrongService wrongService;

    @GetMapping("/subjects")
    public Result<List<Map<String, Object>>> subjects() {
        return Result.success(wrongService.getWrongSubjects(SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/exams")
    public Result<List<Map<String, Object>>> exams(@RequestParam Long subjectId) {
        return Result.success(wrongService.getWrongExams(SecurityUtils.getCurrentUserId(), subjectId));
    }

    @GetMapping("/type-counts")
    public Result<Map<String, Object>> typeCounts(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long examId) {
        return Result.success(wrongService.getWrongTypeCounts(SecurityUtils.getCurrentUserId(), subjectId, examId));
    }

    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long examId,
            @RequestParam(required = false) Integer questionType) {
        return Result.success(wrongService.getWrongList(SecurityUtils.getCurrentUserId(), subjectId, examId, questionType, page, size));
    }

    @GetMapping("/detail/{answerId}")
    public Result<Map<String, Object>> detail(@PathVariable Long answerId) {
        return Result.success(wrongService.getWrongDetail(answerId, SecurityUtils.getCurrentUserId()));
    }

    @DeleteMapping("/{answerId}")
    public Result<Void> remove(@PathVariable Long answerId) {
        wrongService.removeWrong(answerId, SecurityUtils.getCurrentUserId());
        return Result.success("已移除", null);
    }
}
