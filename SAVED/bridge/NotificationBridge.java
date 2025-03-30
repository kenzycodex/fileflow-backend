package com.fileflow.bridge;

import com.fileflow.bridge.core.BridgeInterface;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.model.User;
import com.fileflow.repository.UserRepository;
import com.fileflow.security.UserPrincipal;
import com.fileflow.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Bridge between Java backend and WebView for native notifications
 * Used by desktop and mobile applications
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationBridge implements BridgeInterface {

    private final UserRepository userRepository;

    @Override
    public String getBridgeName() {
        return "notification";
    }

    /**
     * Show native notification
     *
     * @param title notification title
     * @param message notification message
     * @param type notification type (info, success, warning, error)
     * @return success status as JSON
     */
    public String showNotification(String title, String message, String type) {
        try {
            log.info("Show native notification: {} - {} - {}", title, message, type);

            // Implementation depends on platform (desktop/mobile)
            // This would be overridden in the native application

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("title", title);
            result.put("message", message);
            result.put("type", type);
            result.put("timestamp", LocalDateTime.now().toString());

            return JsonUtil.toJson(result);
        } catch (Exception e) {
            log.error("Error showing notification", e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error showing notification: " + e.getMessage());
            error.put("timestamp", LocalDateTime.now().toString());

            return JsonUtil.toJson(error);
        }
    }

    /**
     * Register device for push notifications
     *
     * @param deviceToken device token for push notifications
     * @param deviceType device type (android, ios, desktop)
     * @return registration status as JSON
     */
    public String registerForPushNotifications(String deviceToken, String deviceType) {
        try {
            User currentUser = getCurrentUser();

            // In a real implementation, store the token in the database
            // and associate it with the user's device

            log.info("Registered device {} for push notifications: {}", deviceType, deviceToken);

            Map<String, Object> data = new HashMap<>();
            data.put("deviceToken", deviceToken);
            data.put("deviceType", deviceType);
            data.put("userId", currentUser.getId());

            ApiResponse response = ApiResponse.builder()
                    .success(true)
                    .message("Device registered for push notifications")
                    .data(data)
                    .timestamp(LocalDateTime.now())
                    .build();

            return JsonUtil.toJson(response);
        } catch (Exception e) {
            log.error("Error registering for push notifications", e);

            ApiResponse response = ApiResponse.builder()
                    .success(false)
                    .message("Error registering for push notifications: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();

            return JsonUtil.toJson(response);
        }
    }

    /**
     * Unregister device from push notifications
     *
     * @param deviceToken device token for push notifications
     * @return unregistration status as JSON
     */
    public String unregisterFromPushNotifications(String deviceToken) {
        try {
            User currentUser = getCurrentUser();

            // In a real implementation, remove the token from the database

            log.info("Unregistered device from push notifications: {}", deviceToken);

            Map<String, Object> data = new HashMap<>();
            data.put("deviceToken", deviceToken);
            data.put("userId", currentUser.getId());

            ApiResponse response = ApiResponse.builder()
                    .success(true)
                    .message("Device unregistered from push notifications")
                    .data(data)
                    .timestamp(LocalDateTime.now())
                    .build();

            return JsonUtil.toJson(response);
        } catch (Exception e) {
            log.error("Error unregistering from push notifications", e);

            ApiResponse response = ApiResponse.builder()
                    .success(false)
                    .message("Error unregistering from push notifications: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();

            return JsonUtil.toJson(response);
        }
    }

    /**
     * Check if this bridge is available in the current environment
     * @return true if available
     */
    public boolean isAvailable() {
        // In the web version, this bridge might be available through web notifications
        // But full functionality is only available in native implementations
        return false;
    }

    // Helper methods
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}