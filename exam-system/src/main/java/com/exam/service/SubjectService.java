package com.exam.service;

import com.exam.common.result.PageResult;
import com.exam.dto.request.SubjectAddRequest;
import java.util.List;
import java.util.Map;

public interface SubjectService {
    PageResult<Map<String, Object>> listSubjects(Integer page, Integer size, String subjectName, String grade, Long majorId);
    void addSubject(SubjectAddRequest request);
    void updateSubject(Long id, SubjectAddRequest request);
    void deleteSubject(Long id);
    List<Map<String, Object>> getAllSubjects();
    List<Map<String, Object>> getAllSubjectsByTeacher(Long teacherId);
}
