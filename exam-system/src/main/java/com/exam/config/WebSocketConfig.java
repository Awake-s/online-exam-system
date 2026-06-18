package com.exam.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private WebSocketAuthInterceptor authInterceptor;

    @Autowired
    private StompChannelInterceptor channelInterceptor;

    /**
     * WebSocket 允许的 Origin 列表（配置驱动，与 CORS 共用）
     * <p>
     * 开发环境（application.yml 未设）：默认 "*"（通配，便于本机联调）<br>
     * 生产环境（application-prod.yml / secrets.env）：配置精确域名，禁止通配
     * <p>
     * 示例：<pre>
     *   app:
     *     cors:
     *       allowed-origins: http://124.222.21.219,https://examplatform.online
     * </pre>
     */
    @Value("${app.cors.allowed-origins:*}")
    private String[] allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{10000, 10000})  // 服务端心跳：每10秒检测一次连接存活
                .setTaskScheduler(heartbeatScheduler());
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @org.springframework.context.annotation.Bean
    public org.springframework.scheduling.TaskScheduler heartbeatScheduler() {
        org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler scheduler =
                new org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.setDaemon(true);
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 聊天 WebSocket 端点
        registry.addEndpoint("/ws")
                .setHandshakeHandler(new CustomHandshakeHandler())
                .addInterceptors(authInterceptor)
                .setAllowedOriginPatterns(allowedOrigins)
                .withSockJS();

        // 通知 WebSocket 端点
        registry.addEndpoint("/ws/notification")
                .setHandshakeHandler(new CustomHandshakeHandler())
                .addInterceptors(authInterceptor)
                .setAllowedOriginPatterns(allowedOrigins)
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(channelInterceptor);
    }
}
