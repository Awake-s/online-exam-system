package com.exam.controller;

import com.exam.common.result.Result;
import com.exam.security.SecurityUtils;
import com.exam.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> admin() {
        return Result.success(dashboardService.getAdminDashboard());
    }

    @GetMapping("/teacher")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<Map<String, Object>> teacher() {
        return Result.success(dashboardService.getTeacherDashboard(SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/student")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Map<String, Object>> student() {
        return Result.success(dashboardService.getStudentDashboard(SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/student-trend")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<java.util.List<Map<String, Object>>> studentTrend(@RequestParam(required = false) Long subjectId) {
        return Result.success(dashboardService.getStudentScoreTrend(SecurityUtils.getCurrentUserId(), subjectId));
    }
}
