package com.exam.controller;

import com.exam.common.result.Result;
import com.exam.dto.request.PasswordChangeRequest;
import com.exam.dto.request.ProfileUpdateRequest;
import com.exam.security.SecurityUtils;
import com.exam.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@PreAuthorize("isAuthenticated()")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    @GetMapping("/info")
    public Result<Map<String, Object>> info() {
        return Result.success(profileService.getProfileInfo(SecurityUtils.getCurrentUserId()));
    }

    @PutMapping("/update")
    public Result<Void> update(@Valid @RequestBody ProfileUpdateRequest request) {
        profileService.updateProfile(request, SecurityUtils.getCurrentUserId());
        return Result.success("更新成功", null);
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        profileService.changePassword(request, SecurityUtils.getCurrentUserId());
        return Result.success("密码修改成功", null);
    }

    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String url = profileService.uploadAvatar(file, SecurityUtils.getCurrentUserId());
        Map<String, String> data = new HashMap<>();
        data.put("avatarUrl", url);
        return Result.success(data);
    }
}
