package com.exam.service;

import com.exam.common.result.PageResult;
import java.util.List;
import java.util.Map;

public interface WrongService {
    List<Map<String, Object>> getWrongSubjects(Long userId);
    List<Map<String, Object>> getWrongExams(Long userId, Long subjectId);
    Map<String, Object> getWrongTypeCounts(Long userId, Long subjectId, Long examId);
    PageResult<Map<String, Object>> getWrongList(Long userId, Long subjectId, Long examId, Integer questionType, Integer page, Integer size);
    Map<String, Object> getWrongDetail(Long answerId, Long userId);
    void removeWrong(Long answerId, Long userId);
}
