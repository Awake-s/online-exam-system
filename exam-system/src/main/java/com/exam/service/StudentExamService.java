package com.exam.service;

import com.exam.dto.request.ExamSubmitRequest;
import java.util.List;
import java.util.Map;

public interface StudentExamService {
    List<Map<String, Object>> getMyExams(Long userId, Long classId);
    Map<String, Object> startExam(Long examId, Long userId);
    Map<String, Object> submitExam(ExamSubmitRequest request, Long userId);
    Map<String, Object> getExamResult(Long recordId, Long userId);
    void autoSaveAnswers(ExamSubmitRequest request, Long userId);
    Map<String, Object> recordSwitchScreen(Long recordId, Long userId);
}
