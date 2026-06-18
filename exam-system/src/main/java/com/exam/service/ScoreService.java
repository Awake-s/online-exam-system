package com.exam.service;

import com.exam.common.result.PageResult;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

public interface ScoreService {
    PageResult<Map<String, Object>> getMyScores(Long userId, Integer page, Integer size);
    Map<String, Object> getClassScores(Long examId, Long creatorId);
    void exportScores(Long examId, Long creatorId, HttpServletResponse response);
    Map<String, Object> getScoreAnalysis(Long examId, Long creatorId);
}
