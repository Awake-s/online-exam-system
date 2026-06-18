package com.exam.service;

import com.exam.dto.request.PasswordChangeRequest;
import com.exam.dto.request.ProfileUpdateRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface ProfileService {
    Map<String, Object> getProfileInfo(Long userId);
    void updateProfile(ProfileUpdateRequest request, Long userId);
    void changePassword(PasswordChangeRequest request, Long userId);
    String uploadAvatar(MultipartFile file, Long userId);
}
