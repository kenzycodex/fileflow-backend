package com.fileflow.websocket;

import com.fileflow.security.JwtTokenProvider;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Interceptor to handle JWT authentication for WebSocket connections
 */
@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);
    private final JwtTokenProvider tokenProvider;

    public WebSocketAuthInterceptor(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, @NotNull ServerHttpResponse response,
                                   @NotNull WebSocketHandler wsHandler, @NotNull Map<String, Object> attributes) {
        logger.debug("Processing WebSocket handshake request");

        // Extract token from URL parameter
        String query = request.getURI().getQuery();
        if (query != null) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("token=")) {
                    String token = param.substring(6); // Remove "token="

                    try {
                        // Validate token
                        if (tokenProvider.validateToken(token)) {
                            Long userId = tokenProvider.getUserIdFromJWT(token);
                            attributes.put("userId", userId);

                            // Add token and token type to attributes for later use
                            attributes.put("token", token);
                            attributes.put("tokenType", tokenProvider.getTokenTypeFromJWT(token));

                            logger.debug("WebSocket authentication successful for user ID: {}", userId);
                            return true;
                        } else {
                            logger.warn("Invalid token in WebSocket handshake");
                        }
                    } catch (Exception e) {
                        logger.error("Error validating WebSocket token", e);
                    }
                    break;
                }
            }
        }

        logger.warn("No valid authentication token found in WebSocket request");
        return false; // Reject connection if authentication fails
    }

    @Override
    public void afterHandshake(@NotNull ServerHttpRequest request, @NotNull ServerHttpResponse response,
                               @NotNull WebSocketHandler wsHandler, Exception exception) {
        // Log connection completion
        if (exception != null) {
            logger.error("WebSocket handshake failed", exception);
        } else {
            logger.debug("WebSocket handshake completed successfully");
        }
    }
}