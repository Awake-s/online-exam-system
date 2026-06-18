package com.exam.service;

import com.exam.dto.request.MarkingScoreRequest;
import java.util.List;
import java.util.Map;

public interface MarkingService {
    List<Map<String, Object>> getPendingList(Long examId, Long creatorId);
    Map<String, Object> getMarkingDetail(Long recordId, Long creatorId);
    void markScores(MarkingScoreRequest request, Long creatorId);
    void publishScores(Long examId, Long creatorId);
}
