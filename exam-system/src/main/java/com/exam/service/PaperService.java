package com.exam.service;

import com.exam.common.result.PageResult;
import com.exam.dto.request.PaperCreateRequest;
import com.exam.dto.request.PaperRandomRequest;
import java.util.Map;

public interface PaperService {
    PageResult<Map<String, Object>> listPapers(Integer page, Integer size, Long subjectId, String paperName, Integer status, Long creatorId);
    Map<String, Object> getPaperDetail(Long id, Long creatorId);
    void createPaper(PaperCreateRequest request, Long creatorId);
    void updatePaper(Long id, PaperCreateRequest request, Long creatorId);
    void randomPaper(PaperRandomRequest request, Long creatorId);
    void deletePaper(Long id, Long creatorId);
    void togglePublish(Long id, Long creatorId);
}
