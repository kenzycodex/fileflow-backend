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
import org.springframework.web.multipart.MultipartFile;

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
public class MediaBridge {

    private final UserRepository userRepository;

    /**
     * Open device camera
     *
     * @param cameraType front or back camera
     * @return camera status
     */
    public ApiResponse openCamera(String cameraType) {
        try {
            // This would be implemented in the native application
            // to open the device camera

            log.info("Open camera: {}", cameraType);

            return ApiResponse.builder()
                    .success(true)
                    .message("Camera opened successfully")
                    .data(Map.of("cameraType", cameraType))
                    .timestamp(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("Error opening camera", e);

            return ApiResponse.builder()
                    .success(false)
                    .message("Error opening camera: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * Open media gallery
     *
     * @param mediaType image, video, or all
     * @param multiSelect allow multiple selection
     * @return gallery status
     */
    public ApiResponse openGallery(String mediaType, boolean multiSelect) {
        try {
            // This would be implemented in the native application
            // to open the device gallery

            log.info("Open gallery: {} (multiSelect: {})", mediaType, multiSelect);

            Map<String, Object> data = new HashMap<>();
            data.put("mediaType", mediaType);
            data.put("multiSelect", multiSelect);

            return ApiResponse.builder()
                    .success(true)
                    .message("Gallery opened successfully")
                    .data(data)
                    .timestamp(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("Error opening gallery", e);

            return ApiResponse.builder()
                    .success(false)
                    .message("Error opening gallery: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * Get media from device
     *
     * @param mediaType image, video, or all
     * @param limit maximum number of items to return
     * @return list of media items
     */
    public List<Map<String, Object>> getMediaItems(String mediaType, int limit) {
        // This would be implemented in the native application
        // to get media items from the device

        // Return empty list in this implementation
        return List.of();
    }

    /**
     * Play media file
     *
     * @param mediaType audio or video
     * @param filePath local file path
     * @return playback status
     */
    public ApiResponse playMedia(String mediaType, String filePath) {
        try {
            // This would be implemented in the native application
            // to play media files

            log.info("Play media: {} - {}", mediaType, filePath);

            return ApiResponse.builder()
                    .success(true)
                    .message("Media playback started")
                    .data(Map.of("mediaType", mediaType, "filePath", filePath))
                    .timestamp(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("Error playing media", e);

            return ApiResponse.builder()
                    .success(false)
                    .message("Error playing media: " + e.getMessage())
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