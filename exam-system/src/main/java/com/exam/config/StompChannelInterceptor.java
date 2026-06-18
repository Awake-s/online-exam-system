package com.exam.config;

import com.exam.common.constants.RoleConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Component
public class StompChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null) {
                Long userId = (Long) sessionAttributes.get("userId");
                if (userId != null) {
                    accessor.setUser(new StompPrincipal(String.valueOf(userId)));
                }
            }
            return message;
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            validateSubscribe(accessor);
        }

        return message;
    }

    private void validateSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (destination == null || sessionAttributes == null) {
            return;
        }

        String roleCode = (String) sessionAttributes.get("roleCode");
        Long userId = (Long) sessionAttributes.get("userId");

        // M4 修复：禁止订阅旧的 /topic/online-status 全局广播路径
        // 服务端已改为通过 /user/queue/online-status 按业务可见性定向推送
        // 拦截该路径可防止前端回退旧订阅或攻击者借此枚举全系统用户
        if ("/topic/online-status".equals(destination)) {
            log.warn("Deprecated online-status topic subscribe blocked, userId={}, roleCode={}", userId, roleCode);
            throw new SecurityException("Deprecated destination; use /user/queue/online-status instead");
        }

        // Only admins can subscribe admin broadcast topics.
        if (destination.startsWith("/topic/admin/")) {
            if (!hasRole(roleCode, RoleConstants.ADMIN_CODE)) {
                log.warn("Unauthorized admin topic subscribe blocked, userId={}, roleCode={}, dest={}",
                        userId, roleCode, destination);
                throw new SecurityException("No permission to subscribe admin topic");
            }
            return;
        }

        // Only students can subscribe their own class broadcast topic.
        if (destination.startsWith("/topic/class/")) {
            if (!hasRole(roleCode, RoleConstants.STUDENT_CODE)) {
                log.warn("Unauthorized class topic subscribe blocked, userId={}, roleCode={}, dest={}",
                        userId, roleCode, destination);
                throw new SecurityException("No permission to subscribe class topic");
            }

            Long userClassId = (Long) sessionAttributes.get("classId");
            if (userClassId == null) {
                log.warn("Class topic subscribe blocked due to missing classId, userId={}, dest={}",
                        userId, destination);
                throw new SecurityException("Missing class info for class topic subscribe");
            }

            String[] parts = destination.split("/");
            if (parts.length != 5 || !"topic".equals(parts[1]) || !"class".equals(parts[2]) || !"notification".equals(parts[4])) {
                log.warn("Invalid class topic destination blocked, userId={}, dest={}", userId, destination);
                throw new SecurityException("Invalid class topic destination");
            }

            Long destClassId;
            try {
                destClassId = Long.parseLong(parts[3]);
            } catch (NumberFormatException e) {
                log.warn("Invalid classId in class topic destination blocked, userId={}, dest={}",
                        userId, destination);
                throw new SecurityException("Invalid class topic classId");
            }

            if (!userClassId.equals(destClassId)) {
                log.warn("Cross-class topic subscribe blocked, userId={}, userClassId={}, destClassId={}",
                        userId, userClassId, destClassId);
                throw new SecurityException("No permission to subscribe other class topic");
            }
        }
    }

    private boolean hasRole(String roleCode, String expectedRoleCode) {
        if (roleCode == null || expectedRoleCode == null) {
            return false;
        }
        String normalized = roleCode.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        } else if (normalized.startsWith("R_")) {
            normalized = normalized.substring(2);
        }
        return expectedRoleCode.toUpperCase(Locale.ROOT).equals(normalized);
    }

    public static class StompPrincipal implements Principal {
        private final String name;

        public StompPrincipal(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
