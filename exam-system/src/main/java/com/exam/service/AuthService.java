package com.exam.service;

import java.util.Map;

public interface AuthService {
    Map<String, String> getCaptcha();
    Map<String, Object> login(String username, String password, String captchaKey, String captchaCode);
    Map<String, Object> getUserInfo(Long userId);
    void logout(String token);
}
