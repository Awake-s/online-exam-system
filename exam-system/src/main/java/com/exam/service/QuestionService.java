package com.exam.service;

import com.exam.common.result.PageResult;
import com.exam.dto.request.QuestionAddRequest;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

public interface QuestionService {
    PageResult<Map<String, Object>> listQuestions(Integer page, Integer size, Long subjectId, Integer questionType, Integer difficulty, String keyword, Long creatorId);
    void addQuestion(QuestionAddRequest request, Long creatorId);
    void updateQuestion(Long id, QuestionAddRequest request, Long creatorId);
    String deleteQuestion(Long id, Long creatorId);
    Map<String, Object> batchDeleteQuestions(List<Long> ids, Long creatorId);
    Map<String, Object> getQuestionDetail(Long id, Long creatorId);
    Map<String, Object> importQuestions(MultipartFile file, Long subjectId, Long creatorId);
}
