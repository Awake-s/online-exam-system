package com.exam.service;

import java.util.List;
import java.util.Map;

public interface DashboardService {
    Map<String, Object> getAdminDashboard();
    Map<String, Object> getTeacherDashboard(Long userId);
    Map<String, Object> getStudentDashboard(Long userId, Long classId);
    Map<String, Object> getStudentDashboard(Long userId);
    List<Map<String, Object>> getStudentScoreTrend(Long userId, Long subjectId);
}

