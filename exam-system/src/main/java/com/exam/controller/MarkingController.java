package com.exam.controller;

import com.exam.common.result.Result;
import com.exam.dto.request.MarkingScoreRequest;
import com.exam.security.SecurityUtils;
import com.exam.service.MarkingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/marking")
@PreAuthorize("hasRole('TEACHER')")
public class MarkingController {

    @Autowired
    private MarkingService markingService;

    @GetMapping("/list/{examId}")
    public Result<List<Map<String, Object>>> pendingList(@PathVariable Long examId) {
        return Result.success(markingService.getPendingList(examId, SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/detail/{recordId}")
    public Result<Map<String, Object>> detail(@PathVariable Long recordId) {
        return Result.success(markingService.getMarkingDetail(recordId, SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/score")
    public Result<Void> score(@Valid @RequestBody MarkingScoreRequest request) {
        markingService.markScores(request, SecurityUtils.getCurrentUserId());
        return Result.success("批改成功", null);
    }

    @PostMapping("/publish/{examId}")
    public Result<Void> publish(@PathVariable Long examId) {
        markingService.publishScores(examId, SecurityUtils.getCurrentUserId());
        return Result.success("成绩发布成功", null);
    }
}
