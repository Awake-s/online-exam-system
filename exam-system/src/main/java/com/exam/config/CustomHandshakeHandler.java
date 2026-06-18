package com.exam.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

/**
 * 自定义 WebSocket 握手处理器
 * 在握手阶段设置 Principal，确保 SimpUserRegistry 能正确关联用户与 WebSocket 会话，
 * 使 convertAndSendToUser 能够准确路由消息给目标用户。
 */
public class CustomHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {
        Long userId = (Long) attributes.get("userId");
        if (userId != null) {
            return new StompChannelInterceptor.StompPrincipal(String.valueOf(userId));
        }
        return null;
    }
}
