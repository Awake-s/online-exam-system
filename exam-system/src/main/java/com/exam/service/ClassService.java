package com.exam.service;

import com.exam.common.result.PageResult;
import com.exam.dto.request.ClassAddRequest;
import java.util.List;
import java.util.Map;

public interface ClassService {
    PageResult<Map<String, Object>> listClasses(Integer page, Integer size, String className, String grade, Long majorId);
    void addClass(ClassAddRequest request);
    void updateClass(Long id, ClassAddRequest request);
    void deleteClass(Long id);
    Map<String, Object> getClassDetail(Long id);
    List<Map<String, Object>> getStudentsByClassId(Long classId);
    List<Map<String, Object>> getAllClasses();
    List<Map<String, Object>> getTeacherClasses(Long teacherId);
}
