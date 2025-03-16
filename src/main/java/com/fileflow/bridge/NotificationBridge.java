package com.fileflow.bridge;

import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.model.User;
import com.fileflow.repository.UserRepository;
import com.fileflow.security.UserPrincipal;
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
public class NotificationBridge {

    private final UserRepository userRepository;

    /**
     * Show native notification
     *
     * @param title notification title
     * @param message notification message
     * @param type notification type (info, success, warning, error)
     * @return success status
     */
    public boolean showNotification(String title, String message, String type) {
        try {
            log.info("Show native notification: {} - {}", title, message);

            // Implementation depends on platform (desktop/mobile)
            // This would be overridden in the native application

            return true;
        } catch (Exception e) {
            log.error("Error showing notification", e);
            return false;
        }
    }

    /**
     * Register device for push notifications
     *
     * @param deviceToken device token for push notifications
     * @param deviceType device type (android, ios, desktop)
     * @return registration status
     */
    public ApiResponse registerForPushNotifications(String deviceToken, String deviceType) {
        try {
            User currentUser = getCurrentUser();

            // In a real implementation, store the token in the database
            // and associate it with the user's device

            log.info("Registered device {} for push notifications: {}", deviceType, deviceToken);

            Map<String, Object> data = new HashMap<>();
            data.put("deviceToken", deviceToken);
            data.put("deviceType", deviceType);
            data.put("userId", currentUser.getId());

            return ApiResponse.builder()
                    .success(true)
                    .message("Device registered for push notifications")
                    .data(data)
                    .timestamp(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("Error registering for push notifications", e);

            return ApiResponse.builder()
                    .success(false)
                    .message("Error registering for push notifications: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * Unregister device from push notifications
     *
     * @param deviceToken device token for push notifications
     * @return unregistration status
     */
    public ApiResponse unregisterFromPushNotifications(String deviceToken) {
        try {
            User currentUser = getCurrentUser();

            // In a real implementation, remove the token from the database

            log.info("Unregistered device from push notifications: {}", deviceToken);

            return ApiResponse.builder()
                    .success(true)
                    .message("Device unregistered from push notifications")
                    .timestamp(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("Error unregistering from push notifications", e);

            return ApiResponse.builder()
                    .success(false)
                    .message("Error unregistering from push notifications: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    // Helper methods

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}