package com.exam.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.common.exception.BusinessException;
import com.exam.common.utils.JwtUtils;
import com.exam.entity.EduClass;
import com.exam.entity.SysRole;
import com.exam.entity.SysUser;
import com.exam.mapper.EduClassMapper;
import com.exam.mapper.SysRoleMapper;
import com.exam.mapper.SysUserMapper;
import com.exam.security.LoginRateLimiter;
import com.exam.security.TokenBlacklistService;
import com.exam.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private SysUserMapper userMapper;
    @Autowired
    private SysRoleMapper roleMapper;
    @Autowired
    private EduClassMapper classMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private TokenBlacklistService tokenBlacklistService;
    @Autowired
    private LoginRateLimiter loginRateLimiter;

    private static final Map<String, String> captchaCache = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService captchaCleanupScheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "captcha-cleanup");
                t.setDaemon(true);
                return t;
            });

    @Override
    public Map<String, String> getCaptcha() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 5);
        String key = UUID.randomUUID().toString();
        String code = captcha.getCode();
        captchaCache.put(key, code.toLowerCase());

        captchaCleanupScheduler.schedule(() -> captchaCache.remove(key), 5, TimeUnit.MINUTES);

        Map<String, String> result = new HashMap<>();
        result.put("captchaKey", key);
        result.put("captchaImage", captcha.getImageBase64Data());
        return result;
    }

    @Override
    public Map<String, Object> login(String username, String password, String captchaKey, String captchaCode) {
        if (loginRateLimiter.isLocked(username)) {
            long minutes = loginRateLimiter.getRemainingLockMinutes(username);
            throw new BusinessException("登录失败次数过多，请" + minutes + "分钟后再试");
        }

        // 验证码校验（可选，前端使用拖拽验证滑块代替图形验证码）
        if (captchaKey != null && !captchaKey.isEmpty()) {
            String cachedCode = captchaCache.get(captchaKey);
            if (cachedCode == null || !cachedCode.equalsIgnoreCase(captchaCode)) {
                throw new BusinessException("验证码错误或已过期");
            }
            captchaCache.remove(captchaKey);
        }

        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) {
            loginRateLimiter.recordFailure(username);
            throw new BusinessException("用户名或密码错误");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            loginRateLimiter.recordFailure(username);
            throw new BusinessException("用户名或密码错误");
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }

        loginRateLimiter.recordSuccess(username);

        SysRole role = roleMapper.selectById(user.getRoleId());
        String roleCode = role != null ? role.getRoleCode() : "STUDENT";
        String roleName = role != null ? role.getRoleName() : "学生";

        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), roleCode);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("realName", user.getRealName());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("roleId", user.getRoleId());
        userInfo.put("roleCode", roleCode);
        userInfo.put("roleName", roleName);
        userInfo.put("classId", user.getClassId());
        if (user.getClassId() != null) {
            EduClass eduClass = classMapper.selectById(user.getClassId());
            userInfo.put("className", eduClass != null ? eduClass.getClassName() : null);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userInfo", userInfo);
        return result;
    }

    @Override
    public Map<String, Object> getUserInfo(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        SysRole role = roleMapper.selectById(user.getRoleId());
        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("realName", user.getRealName());
        info.put("avatar", user.getAvatar());
        info.put("email", user.getEmail());
        info.put("phone", user.getPhone());
        info.put("gender", user.getGender());
        info.put("roleId", user.getRoleId());
        info.put("roleCode", role != null ? role.getRoleCode() : null);
        info.put("roleName", role != null ? role.getRoleName() : null);
        info.put("classId", user.getClassId());
        if (user.getClassId() != null) {
            EduClass eduClass = classMapper.selectById(user.getClassId());
            info.put("className", eduClass != null ? eduClass.getClassName() : null);
        } else {
            info.put("className", null);
        }
        return info;
    }

    @Override
    public void logout(String token) {
        if (token != null && jwtUtils.validateToken(token)) {
            long expirationTime = jwtUtils.getExpirationTime(token);
            tokenBlacklistService.add(token, expirationTime);
        }
    }
}
