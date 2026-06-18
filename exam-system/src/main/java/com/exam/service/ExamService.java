package com.exam.service;

import com.exam.common.result.PageResult;
import com.exam.dto.request.ExamPublishRequest;
import java.util.List;
import java.util.Map;

public interface ExamService {
    PageResult<Map<String, Object>> listExams(Integer page, Integer size, String examName, Integer status, Long classId, Long subjectId, Long creatorId);
    void publishExam(ExamPublishRequest request, Long creatorId);
    void updateExam(Long id, ExamPublishRequest request, Long creatorId);
    void deleteExam(Long id, Long creatorId);
    List<Map<String, Object>> getExamRecords(Long examId, Long creatorId);
}
