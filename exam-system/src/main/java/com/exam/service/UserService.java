package com.exam.service;

import com.exam.common.result.PageResult;
import com.exam.dto.request.UserAddRequest;
import com.exam.dto.request.UserUpdateRequest;
import java.util.Map;

public interface UserService {
    PageResult<Map<String, Object>> listUsers(Integer page, Integer size, String username, String realName, Long roleId, Long classId, Long majorId, Long subjectId, String grade, Integer status);
    void addUser(UserAddRequest request);
    void updateUser(UserUpdateRequest request);
    void deleteUser(Long id, Long currentUserId);
    void updateStatus(Long id, Integer status);
    void resetPassword(Long id);
}
