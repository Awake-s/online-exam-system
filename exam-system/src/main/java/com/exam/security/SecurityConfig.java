package com.exam.security;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .headers()
                .frameOptions().sameOrigin() // 允许同源iframe（SockJS需要），防止跨域点击劫持
            .and()
            .authorizeRequests()
                .antMatchers("/api/auth/login", "/api/auth/captcha", "/api/auth/logout").permitAll()
                .antMatchers("/ws/**").permitAll()
                .antMatchers("/uploads/**").permitAll()
                // Actuator 健康检查放行（Nginx 已限制仅 127.0.0.1 访问，无对外暴露风险）
                .antMatchers("/actuator/health", "/actuator/info").permitAll()
                .anyRequest().authenticated()
            .and()
            .exceptionHandling()
                .authenticationEntryPoint((req, resp, e) -> {
                    resp.setContentType("application/json;charset=UTF-8");
                    resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    Map<String, Object> r = new HashMap<>();
                    r.put("code", 401);
                    r.put("message", "未登录或Token已过期");
                    resp.getWriter().write(new ObjectMapper().writeValueAsString(r));
                })
                .accessDeniedHandler((req, resp, e) -> {
                    resp.setContentType("application/json;charset=UTF-8");
                    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    Map<String, Object> r = new HashMap<>();
                    r.put("code", 403);
                    r.put("message", "无权限访问");
                    resp.getWriter().write(new ObjectMapper().writeValueAsString(r));
                })
            .and()
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
