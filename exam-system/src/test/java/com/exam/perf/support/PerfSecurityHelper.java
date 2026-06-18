package com.exam.perf.support;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

/**
 * 性能压测专用 Security 上下文 Mock 工具。
 *
 * <p>项目中部分 Service 内部可能调用 {@code SecurityUtils.getCurrentUserId()} 或检查
 * Security 角色注解，本类用于在测试代码中预先填充一个有效的 admin 身份，避免
 * NullPointerException 或权限校验失败。
 *
 * <p>使用模式：
 * <pre>
 * &#64;BeforeEach
 * void setUp() {
 *     PerfSecurityHelper.loginAsAdmin(adminUserId);
 * }
 *
 * &#64;AfterEach
 * void tearDown() {
 *     PerfSecurityHelper.logout();
 * }
 * </pre>
 *
 * <p>对齐项目 {@code JwtAuthenticationFilter} 的 Authentication 构造方式：principal 是 Long userId，
 * authorities 是 ROLE_ 前缀的角色字符串。
 */
public class PerfSecurityHelper {

    /** 模拟管理员登录（principal=adminUserId, authority=ROLE_ADMIN） */
    public static void loginAsAdmin(Long adminUserId) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                adminUserId,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /** 模拟教师登录 */
    public static void loginAsTeacher(Long teacherUserId) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                teacherUserId,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_TEACHER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /** 模拟学生登录 */
    public static void loginAsStudent(Long studentUserId) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                studentUserId,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_STUDENT")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /** 退出登录（清空 SecurityContext） */
    public static void logout() {
        SecurityContextHolder.clearContext();
    }
}
