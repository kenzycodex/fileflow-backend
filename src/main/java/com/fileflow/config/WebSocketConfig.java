package com.fileflow.config;

import com.fileflow.websocket.FileFlowWebSocketHandler;
import com.fileflow.websocket.WebSocketAuthInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * Configuration for WebSocket support with JWT authentication integration
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final FileFlowWebSocketHandler webSocketHandler;
    private final WebSocketAuthInterceptor authInterceptor;

    @Value("${app.security.allowed-origins}")
    private String[] allowedOrigins;

    public WebSocketConfig(FileFlowWebSocketHandler webSocketHandler,
                           WebSocketAuthInterceptor authInterceptor) {
        this.webSocketHandler = webSocketHandler;
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/api/v1/ws")
                .addInterceptors(authInterceptor)
                .setAllowedOrigins(allowedOrigins) // Use configured origins from application.properties
                .setAllowedOriginPatterns("*") // Alternatively, use origin patterns which work with credentials
                .withSockJS()
                .setSessionCookieNeeded(false)
                .setWebSocketEnabled(true)
                .setHeartbeatTime(25000);
    }

    /**
     * Configure WebSocket container settings
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(8192);
        container.setMaxBinaryMessageBufferSize(1024 * 1024 * 10); // 10MB
        container.setMaxSessionIdleTimeout(60000L); // 60 seconds
        return container;
    }
}