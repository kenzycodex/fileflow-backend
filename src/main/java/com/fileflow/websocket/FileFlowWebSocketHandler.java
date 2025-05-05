package com.fileflow.websocket;

import com.fileflow.security.JwtTokenProvider;
import com.fileflow.service.user.UserService;
import com.fileflow.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for FileFlow application
 */
@Component
@Slf4j
public class FileFlowWebSocketHandler extends TextWebSocketHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final WebSocketEventPublisher eventPublisher;

    // Store active sessions by user ID
    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    // Store user IDs by session ID
    private final Map<String, Long> sessionUsers = new ConcurrentHashMap<>();

    public FileFlowWebSocketHandler(JwtTokenProvider jwtTokenProvider,
                                    UserService userService,
                                    WebSocketEventPublisher eventPublisher) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void afterConnectionEstablished(@NotNull WebSocketSession session) {
        try {
            // Extract JWT token from session attributes or URL parameters
            String token = extractToken(session);
            if (token != null && jwtTokenProvider.validateToken(token)) {
                Long userId = jwtTokenProvider.getUserIdFromJWT(token);

                // Store session information
                userSessions.put(userId, session);
                sessionUsers.put(session.getId(), userId);

                log.info("WebSocket connection established for user: {}", userId);

                // Notify the user about the successful connection
                sendMessage(session, new WebSocketMessage(
                        WebSocketMessageType.CONNECTION_ESTABLISHED,
                        Map.of("userId", userId)
                ));
            } else {
                log.warn("Closing WebSocket connection due to invalid token");
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Invalid token"));
            }
        } catch (Exception e) {
            log.error("Error establishing WebSocket connection", e);
            try {
                session.close(CloseStatus.SERVER_ERROR.withReason("Connection error"));
            } catch (IOException ex) {
                log.error("Error closing WebSocket session", ex);
            }
        }
    }

    @Override
    protected void handleTextMessage(@NotNull WebSocketSession session, @NotNull TextMessage message) {
        try {
            Long userId = sessionUsers.get(session.getId());
            if (userId == null) {
                log.warn("Received message from unidentified session: {}", session.getId());
                return;
            }

            // Parse the incoming message
            WebSocketMessage webSocketMessage = JsonUtil.fromJson(
                    message.getPayload(), WebSocketMessage.class
            );

            // Process the message based on its type
            switch (webSocketMessage.getType()) {
                case PING:
                    handlePingMessage(session);
                    break;
                case SUBSCRIBE_TO_FILE_EVENTS:
                    handleSubscribeToFileEvents(userId, webSocketMessage);
                    break;
                case SUBSCRIBE_TO_FOLDER_EVENTS:
                    handleSubscribeToFolderEvents(userId, webSocketMessage);
                    break;
                default:
                    log.warn("Unhandled message type: {}", webSocketMessage.getType());
            }
        } catch (Exception e) {
            log.error("Error handling WebSocket message", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NotNull CloseStatus status) {
        Long userId = sessionUsers.remove(session.getId());
        if (userId != null) {
            userSessions.remove(userId);
            log.info("WebSocket connection closed for user: {}", userId);

            // Clean up any subscriptions
            eventPublisher.removeUserSubscriptions(userId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, @NotNull Throwable exception) {
        log.error("Transport error in WebSocket session: {}", session.getId(), exception);
        try {
            session.close(CloseStatus.SERVER_ERROR.withReason("Transport error"));
        } catch (IOException e) {
            log.error("Error closing WebSocket session after transport error", e);
        }
    }

    /**
     * Send a message to a specific user
     */
    public void sendMessageToUser(Long userId, WebSocketMessage message) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            sendMessage(session, message);
        }
    }

    /**
     * Send a message to a specific user about file changes
     */
    public void sendFileChangeNotification(Long fileId, Long userId, WebSocketActionType action, Map<String, Object> data) {
        WebSocketMessage message = new WebSocketMessage(
                WebSocketMessageType.FILE_EVENT,
                Map.of(
                        "fileId", fileId,
                        "action", action.name(),
                        "data", data
                )
        );
        sendMessageToUser(userId, message);
    }

    /**
     * Send a message to a specific user about folder changes
     */
    public void sendFolderChangeNotification(Long folderId, Long userId, WebSocketActionType action, Map<String, Object> data) {
        WebSocketMessage message = new WebSocketMessage(
                WebSocketMessageType.FOLDER_EVENT,
                Map.of(
                        "folderId", folderId,
                        "action", action.name(),
                        "data", data
                )
        );
        sendMessageToUser(userId, message);
    }

    public boolean isUserConnected(Long userId) {
        WebSocketSession session = userSessions.get(userId);
        return session != null && session.isOpen();
    }

    // Helper methods

    private String extractToken(WebSocketSession session) {
        // Try to get from session attributes first
        String token = (String) session.getAttributes().get("token");

        // If not found, try URL parameters
        if (token == null) {
            String query = Objects.requireNonNull(session.getUri()).getQuery();
            if (query != null) {
                String[] params = query.split("&");
                for (String param : params) {
                    String[] keyValue = param.split("=");
                    if (keyValue.length == 2 && "token".equals(keyValue[0])) {
                        token = keyValue[1];
                        break;
                    }
                }
            }
        }

        return token;
    }

    private void sendMessage(WebSocketSession session, WebSocketMessage message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(JsonUtil.toJson(message)));
            }
        } catch (IOException e) {
            log.error("Error sending message to WebSocket session: {}", session.getId(), e);
        }
    }

    private void handlePingMessage(WebSocketSession session) {
        sendMessage(session, new WebSocketMessage(WebSocketMessageType.PONG, null));
    }

    private void handleSubscribeToFileEvents(Long userId, WebSocketMessage message) {
        if (message.getData() != null && message.getData().containsKey("fileId")) {
            Long fileId = Long.valueOf(message.getData().get("fileId").toString());
            eventPublisher.subscribeToFileEvents(userId, fileId);
            log.info("User {} subscribed to file events for file {}", userId, fileId);
        }
    }

    private void handleSubscribeToFolderEvents(Long userId, WebSocketMessage message) {
        if (message.getData() != null && message.getData().containsKey("folderId")) {
            Long folderId = Long.valueOf(message.getData().get("folderId").toString());
            eventPublisher.subscribeToFolderEvents(userId, folderId);
            log.info("User {} subscribed to folder events for folder {}", userId, folderId);
        }
    }
}