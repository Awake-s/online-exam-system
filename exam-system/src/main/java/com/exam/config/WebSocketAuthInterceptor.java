package com.exam.config;

import com.exam.common.utils.JwtUtils;
import com.exam.entity.SysUser;
import com.exam.mapper.SysUserMapper;
import com.exam.security.TokenBlacklistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            String token = servletRequest.getServletRequest().getParameter("token");
            if (token != null && jwtUtils.validateToken(token) && !tokenBlacklistService.isBlacklisted(token)) {
                Long userId = jwtUtils.getUserId(token);
                SysUser user = userMapper.selectById(userId);
                // L3-WS-M1-1：与 JwtAuthenticationFilter 对齐，禁用用户（status != 1）
                // 即使 token 仍在有效期内也不得建立 WebSocket 连接，否则能继续：
                //   1. 接收 /user/queue/messages 推送
                //   2. 触发 /user/queue/online-status 广播（泄露在线状态给其他观察者）
                if (user == null || user.getStatus() == null || user.getStatus() != 1) {
                    return false;
                }
                String roleCode = jwtUtils.getRoleCode(token);
                attributes.put("userId", userId);
                attributes.put("roleCode", roleCode);
                // 查询用户的 classId，用于订阅级权限校验
                if (user.getClassId() != null) {
                    attributes.put("classId", user.getClassId());
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
