package com.exam.controller;

import com.exam.common.result.PageResult;
import com.exam.common.result.Result;
import com.exam.security.SecurityUtils;
import com.exam.service.ScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/score")
public class ScoreController {

    @Autowired
    private ScoreService scoreService;

    @GetMapping("/my-scores")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<PageResult<Map<String, Object>>> myScores(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(scoreService.getMyScores(SecurityUtils.getCurrentUserId(), page, size));
    }

    @GetMapping("/class/{examId}")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<Map<String, Object>> classScores(@PathVariable Long examId) {
        return Result.success(scoreService.getClassScores(examId, SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/export/{examId}")
    @PreAuthorize("hasRole('TEACHER')")
    public void exportScores(@PathVariable Long examId, HttpServletResponse response) {
        scoreService.exportScores(examId, SecurityUtils.getCurrentUserId(), response);
    }

    @GetMapping("/analysis/{examId}")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<Map<String, Object>> analysis(@PathVariable Long examId) {
        return Result.success(scoreService.getScoreAnalysis(examId, SecurityUtils.getCurrentUserId()));
    }
}
