package com.fileflow.controller;

import com.fileflow.dto.request.websocket.WebSocketSubscriptionRequest;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.repository.UserRepository;
import com.fileflow.security.UserPrincipal;
import com.fileflow.service.websocket.WebSocketService;
import com.fileflow.websocket.WebSocketEventPublisher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/websocket")
@RequiredArgsConstructor
@Tag(name = "WebSocket Management", description = "WebSocket subscription and management API")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class WebSocketController {

    private final WebSocketEventPublisher eventPublisher;
    private final WebSocketService webSocketService;
    private final UserRepository userRepository;

    @PostMapping("/subscribe/file")
    @Operation(summary = "Subscribe to file events")
    public ResponseEntity<ApiResponse> subscribeToFileEvents(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody WebSocketSubscriptionRequest request) {
        try {
            Long userId = userPrincipal.getId();
            Long fileId = request.getItemId();

            webSocketService.subscribeToFileEvents(userId, fileId);

            return ResponseEntity.ok(buildSuccessResponse(
                    "Subscribed to file events",
                    Map.of("userId", userId, "fileId", fileId, "subscriptionType", "FILE_EVENTS")
            ));
        } catch (Exception e) {
            log.error("Error subscribing to file events", e);
            return ResponseEntity.internalServerError().body(buildErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/subscribe/folder")
    @Operation(summary = "Subscribe to folder events")
    public ResponseEntity<ApiResponse> subscribeToFolderEvents(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody WebSocketSubscriptionRequest request) {
        try {
            Long userId = userPrincipal.getId();
            Long folderId = request.getItemId();

            webSocketService.subscribeToFolderEvents(userId, folderId);

            return ResponseEntity.ok(buildSuccessResponse(
                    "Subscribed to folder events",
                    Map.of("userId", userId, "folderId", folderId, "subscriptionType", "FOLDER_EVENTS")
            ));
        } catch (Exception e) {
            log.error("Error subscribing to folder events", e);
            return ResponseEntity.internalServerError().body(buildErrorResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/unsubscribe/file/{fileId}")
    @Operation(summary = "Unsubscribe from file events")
    public ResponseEntity<ApiResponse> unsubscribeFromFileEvents(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long fileId) {
        try {
            Long userId = userPrincipal.getId();
            webSocketService.unsubscribeFromFileEvents(userId, fileId);
            return ResponseEntity.ok(buildSuccessResponse("Unsubscribed from file events"));
        } catch (Exception e) {
            log.error("Error unsubscribing from file events", e);
            return ResponseEntity.internalServerError().body(buildErrorResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/unsubscribe/folder/{folderId}")
    @Operation(summary = "Unsubscribe from folder events")
    public ResponseEntity<ApiResponse> unsubscribeFromFolderEvents(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long folderId) {
        try {
            Long userId = userPrincipal.getId();
            webSocketService.unsubscribeFromFolderEvents(userId, folderId);
            return ResponseEntity.ok(buildSuccessResponse("Unsubscribed from folder events"));
        } catch (Exception e) {
            log.error("Error unsubscribing from folder events", e);
            return ResponseEntity.internalServerError().body(buildErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/unsubscribe/all")
    @Operation(summary = "Unsubscribe from all events")
    public ResponseEntity<ApiResponse> unsubscribeFromAllEvents(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            Long userId = userPrincipal.getId();
            webSocketService.removeUserSubscriptions(userId);
            return ResponseEntity.ok(buildSuccessResponse("Unsubscribed from all events"));
        } catch (Exception e) {
            log.error("Error unsubscribing from all events", e);
            return ResponseEntity.internalServerError().body(buildErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/status")
    @Operation(summary = "Get WebSocket connection status")
    public ResponseEntity<ApiResponse> getWebSocketStatus(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            Long userId = userPrincipal.getId();
            boolean isConnected = webSocketService.isUserConnected(userId);

            return ResponseEntity.ok(buildSuccessResponse(
                    "WebSocket connection status",
                    Map.of(
                            "userId", userId,
                            "connected", isConnected,
                            "protocol", "ws",
                            "endpoint", "/ws/fileflow"
                    )
            ));
        } catch (Exception e) {
            log.error("Error getting WebSocket status", e);
            return ResponseEntity.internalServerError().body(buildErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "Get detailed WebSocket statistics")
    public ResponseEntity<ApiResponse> getWebSocketStats(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            Map<String, Object> stats = webSocketService.getWebSocketStats();
            return ResponseEntity.ok(buildSuccessResponse("WebSocket statistics", stats));
        } catch (Exception e) {
            log.error("Error getting WebSocket stats", e);
            return ResponseEntity.internalServerError().body(buildErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/notification")
    @Operation(summary = "Send a system notification to a user")
    public ResponseEntity<ApiResponse> sendSystemNotification(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam String title,
            @RequestParam String message,
            @RequestParam(defaultValue = "info") String level) {
        try {
            Long userId = userPrincipal.getId();
            webSocketService.sendSystemNotification(userId, title, message, level);
            return ResponseEntity.ok(buildSuccessResponse("System notification sent"));
        } catch (Exception e) {
            log.error("Error sending system notification", e);
            return ResponseEntity.internalServerError().body(buildErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/test-connection")
    @Operation(summary = "Test WebSocket connection")
    public ResponseEntity<ApiResponse> testConnection(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            Long userId = userPrincipal.getId();
            webSocketService.sendSystemNotification(
                    userId,
                    "Connection Test",
                    "Your WebSocket connection is working properly.",
                    "success"
            );
            return ResponseEntity.ok(buildSuccessResponse("Test message sent via WebSocket"));
        } catch (Exception e) {
            log.error("Error testing WebSocket connection", e);
            return ResponseEntity.internalServerError().body(buildErrorResponse(e.getMessage()));
        }
    }

    // Helper methods for building responses
    private ApiResponse buildSuccessResponse(String message) {
        return ApiResponse.builder()
                .success(true)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private ApiResponse buildSuccessResponse(String message, Map<String, Object> data) {
        return ApiResponse.builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private ApiResponse buildErrorResponse(String errorMessage) {
        return ApiResponse.builder()
                .success(false)
                .message(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();
    }
}