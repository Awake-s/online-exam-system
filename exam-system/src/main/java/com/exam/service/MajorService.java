package com.exam.service;

import com.exam.common.result.PageResult;
import com.exam.dto.request.MajorAddRequest;
import java.util.List;
import java.util.Map;

public interface MajorService {
    PageResult<Map<String, Object>> listMajors(Integer page, Integer size, String majorName);
    void addMajor(MajorAddRequest request);
    void updateMajor(Long id, MajorAddRequest request);
    void deleteMajor(Long id);
    List<Map<String, Object>> getAllMajors();
}
