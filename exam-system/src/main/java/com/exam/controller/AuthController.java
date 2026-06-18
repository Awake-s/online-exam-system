package com.exam.controller;

import com.exam.common.result.Result;
import com.exam.dto.request.LoginRequest;
import com.exam.security.SecurityUtils;
import com.exam.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @GetMapping("/captcha")
    public Result<Map<String, String>> getCaptcha() {
        return Result.success(authService.getCaptcha());
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        Map<String, Object> data = authService.login(
                request.getUsername(), request.getPassword(),
                request.getCaptchaKey(), request.getCaptchaCode());
        return Result.success("登录成功", data);
    }

    @GetMapping("/info")
    public Result<Map<String, Object>> getUserInfo() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(authService.getUserInfo(userId));
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            authService.logout(header.substring(7));
        }
        return Result.success("登出成功", null);
    }
}
