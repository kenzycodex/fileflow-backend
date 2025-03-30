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
import java.util.List;
import java.util.Map;

/**
 * Bridge between Java backend and WebView for media operations
 * Used by desktop and mobile applications
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MediaBridge implements BridgeInterface {

    private final UserRepository userRepository;

    @Override
    public String getBridgeName() {
        return "media";
    }

    /**
     * Open device camera
     *
     * @param cameraType front or back camera
     * @return camera status as JSON
     */
    public String openCamera(String cameraType) {
        try {
            // This would be implemented in the native application
            // to open the device camera
            log.info("Open camera request: {}", cameraType);

            ApiResponse response = ApiResponse.builder()
                    .success(true)
                    .message("Camera opened successfully")
                    .data(Map.of("cameraType", cameraType))
                    .timestamp(LocalDateTime.now())
                    .build();

            return JsonUtil.toJson(response);
        } catch (Exception e) {
            log.error("Error opening camera", e);

            ApiResponse response = ApiResponse.builder()
                    .success(false)
                    .message("Error opening camera: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();

            return JsonUtil.toJson(response);
        }
    }

    /**
     * Open media gallery
     *
     * @param mediaType image, video, or all
     * @param multiSelect allow multiple selection
     * @return gallery status as JSON
     */
    public String openGallery(String mediaType, String multiSelect) {
        try {
            boolean allowMultiSelect = Boolean.parseBoolean(multiSelect);

            // This would be implemented in the native application
            // to open the device gallery
            log.info("Open gallery request: {} (multiSelect: {})", mediaType, allowMultiSelect);

            Map<String, Object> data = new HashMap<>();
            data.put("mediaType", mediaType);
            data.put("multiSelect", allowMultiSelect);

            ApiResponse response = ApiResponse.builder()
                    .success(true)
                    .message("Gallery opened successfully")
                    .data(data)
                    .timestamp(LocalDateTime.now())
                    .build();

            return JsonUtil.toJson(response);
        } catch (Exception e) {
            log.error("Error opening gallery", e);

            ApiResponse response = ApiResponse.builder()
                    .success(false)
                    .message("Error opening gallery: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();

            return JsonUtil.toJson(response);
        }
    }

    /**
     * Get media from device
     *
     * @param mediaType image, video, or all
     * @param limit maximum number of items to return
     * @return list of media items as JSON
     */
    public String getMediaItems(String mediaType, String limit) {
        try {
            int itemLimit = Integer.parseInt(limit);

            // This would be implemented in the native application
            // to get media items from the device
            log.info("Get media items request: {} (limit: {})", mediaType, itemLimit);

            // Return empty list in this implementation
            Map<String, Object> result = new HashMap<>();
            result.put("items", List.of());
            result.put("mediaType", mediaType);
            result.put("limit", itemLimit);
            result.put("count", 0);
            result.put("message", "No media items found");
            result.put("timestamp", LocalDateTime.now().toString());

            return JsonUtil.toJson(result);
        } catch (Exception e) {
            log.error("Error getting media items", e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error getting media items: " + e.getMessage());
            error.put("timestamp", LocalDateTime.now().toString());

            return JsonUtil.toJson(error);
        }
    }

    /**
     * Play media file
     *
     * @param mediaType audio or video
     * @param filePath local file path
     * @return playback status as JSON
     */
    public String playMedia(String mediaType, String filePath) {
        try {
            // This would be implemented in the native application
            // to play media files
            log.info("Play media request: {} - {}", mediaType, filePath);

            ApiResponse response = ApiResponse.builder()
                    .success(true)
                    .message("Media playback started")
                    .data(Map.of("mediaType", mediaType, "filePath", filePath))
                    .timestamp(LocalDateTime.now())
                    .build();

            return JsonUtil.toJson(response);
        } catch (Exception e) {
            log.error("Error playing media", e);

            ApiResponse response = ApiResponse.builder()
                    .success(false)
                    .message("Error playing media: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();

            return JsonUtil.toJson(response);
        }
    }

    /**
     * Capture photo using device camera
     *
     * @param cameraType front or back camera
     * @param savePath local path to save the photo
     * @return capture status as JSON
     */
    public String capturePhoto(String cameraType, String savePath) {
        try {
            // This would be implemented in the native application
            log.info("Capture photo request: {} - save to: {}", cameraType, savePath);

            ApiResponse response = ApiResponse.builder()
                    .success(true)
                    .message("Photo capture simulated")
                    .data(Map.of(
                            "cameraType", cameraType,
                            "savePath", savePath,
                            "timestamp", LocalDateTime.now().toString()
                    ))
                    .timestamp(LocalDateTime.now())
                    .build();

            return JsonUtil.toJson(response);
        } catch (Exception e) {
            log.error("Error capturing photo", e);

            ApiResponse response = ApiResponse.builder()
                    .success(false)
                    .message("Error capturing photo: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();

            return JsonUtil.toJson(response);
        }
    }

    /**
     * Record video using device camera
     *
     * @param cameraType front or back camera
     * @param maxDuration maximum duration in seconds
     * @param savePath local path to save the video
     * @return recording status as JSON
     */
    public String recordVideo(String cameraType, String maxDuration, String savePath) {
        try {
            int duration = Integer.parseInt(maxDuration);

            // This would be implemented in the native application
            log.info("Record video request: {} - duration: {}s - save to: {}",
                    cameraType, duration, savePath);

            ApiResponse response = ApiResponse.builder()
                    .success(true)
                    .message("Video recording simulated")
                    .data(Map.of(
                            "cameraType", cameraType,
                            "maxDuration", duration,
                            "savePath", savePath,
                            "timestamp", LocalDateTime.now().toString()
                    ))
                    .timestamp(LocalDateTime.now())
                    .build();

            return JsonUtil.toJson(response);
        } catch (Exception e) {
            log.error("Error recording video", e);

            ApiResponse response = ApiResponse.builder()
                    .success(false)
                    .message("Error recording video: " + e.getMessage())
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
        // In the web version, this bridge is typically not available
        // It will be overridden in native implementations
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