package com.exam.controller;

import com.exam.common.result.PageResult;
import com.exam.common.result.Result;
import com.exam.dto.request.ExamPublishRequest;
import com.exam.security.SecurityUtils;
import com.exam.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exam")
@PreAuthorize("hasRole('TEACHER')")
public class ExamController {

    @Autowired
    private ExamService examService;

    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String examName,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long subjectId) {
        return Result.success(examService.listExams(page, size, examName, status, classId, subjectId, SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/add")
    public Result<Void> add(@Valid @RequestBody ExamPublishRequest request) {
        examService.publishExam(request, SecurityUtils.getCurrentUserId());
        return Result.success("发布成功", null);
    }

    @PutMapping("/update/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ExamPublishRequest request) {
        examService.updateExam(id, request, SecurityUtils.getCurrentUserId());
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        examService.deleteExam(id, SecurityUtils.getCurrentUserId());
        return Result.success("删除成功", null);
    }

    @GetMapping("/records/{examId}")
    public Result<List<Map<String, Object>>> records(@PathVariable Long examId) {
        return Result.success(examService.getExamRecords(examId, SecurityUtils.getCurrentUserId()));
    }
}
