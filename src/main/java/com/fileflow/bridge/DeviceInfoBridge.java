package com.fileflow.bridge;

import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.model.Device;
import com.fileflow.model.User;
import com.fileflow.repository.DeviceRepository;
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
 * Bridge between Java backend and WebView for device information
 * Used by desktop and mobile applications
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeviceInfoBridge {

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;

    /**
     * Register device
     *
     * @param deviceName device name
     * @param deviceType device type (android, ios, desktop)
     * @param platform platform information
     * @return registration status
     */
    public ApiResponse registerDevice(String deviceName, String deviceType, String platform) {
        try {
            User currentUser = getCurrentUser();

            // Create device record
            Device device = Device.builder()
                    .user(currentUser)
                    .deviceName(deviceName)
                    .deviceType(deviceType)
                    .platform(platform)
                    .lastSyncDate(LocalDateTime.now())
                    .lastActive(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .build();

            Device savedDevice = deviceRepository.save(device);

            log.info("Registered device: {} - {}", deviceType, deviceName);

            Map<String, Object> data = new HashMap<>();
            data.put("deviceId", savedDevice.getId());
            data.put("deviceName", savedDevice.getDeviceName());
            data.put("deviceType", savedDevice.getDeviceType());
            data.put("platform", savedDevice.getPlatform());

            return ApiResponse.builder()
                    .success(true)
                    .message("Device registered successfully")
                    .data(data)
                    .timestamp(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("Error registering device", e);

            return ApiResponse.builder()
                    .success(false)
                    .message("Error registering device: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * Update device information
     *
     * @param deviceId device ID
     * @param deviceName new device name
     * @param pushToken new push token
     * @return update status
     */
    public ApiResponse updateDevice(Long deviceId, String deviceName, String pushToken) {
        try {
            User currentUser = getCurrentUser();

            Device device = deviceRepository.findById(deviceId)
                    .orElseThrow(() -> new RuntimeException("Device not found: " + deviceId));

            // Ensure device belongs to current user
            if (!device.getUser().getId().equals(currentUser.getId())) {
                throw new RuntimeException("Device does not belong to current user");
            }

            // Update device
            if (deviceName != null && !deviceName.isEmpty()) {
                device.setDeviceName(deviceName);
            }

            if (pushToken != null) {
                device.setPushToken(pushToken);
            }

            device.setLastActive(LocalDateTime.now());

            Device updatedDevice = deviceRepository.save(device);

            log.info("Updated device: {}", updatedDevice.getDeviceName());

            return ApiResponse.builder()
                    .success(true)
                    .message("Device updated successfully")
                    .timestamp(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("Error updating device", e);

            return ApiResponse.builder()
                    .success(false)
                    .message("Error updating device: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * Get device information
     *
     * @return device information
     */
    public Map<String, Object> getDeviceInfo() {
        Map<String, Object> info = new HashMap<>();

        // This would be overridden in the native application
        // to provide actual device information

        info.put("os", System.getProperty("os.name"));
        info.put("osVersion", System.getProperty("os.version"));
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        info.put("totalMemory", Runtime.getRuntime().totalMemory());
        info.put("freeMemory", Runtime.getRuntime().freeMemory());
        info.put("maxMemory", Runtime.getRuntime().maxMemory());

        return info;
    }

    /**
     * Get network information
     *
     * @return network information
     */
    public Map<String, Object> getNetworkInfo() {
        Map<String, Object> info = new HashMap<>();

        // This would be overridden in the native application
        // to provide actual network information

        info.put("online", true);
        info.put("networkType", "unknown");
        info.put("connectionQuality", "unknown");

        return info;
    }

    // Helper methods

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}