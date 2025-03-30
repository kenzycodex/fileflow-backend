package com.fileflow.bridge;

import com.fileflow.bridge.core.BridgeInterface;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.model.Device;
import com.fileflow.model.User;
import com.fileflow.repository.DeviceRepository;
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
 * Bridge between Java backend and WebView for device information
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeviceInfoBridge implements BridgeInterface {

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;

    @Override
    public String getBridgeName() {
        return "device";
    }

    /**
     * Register device
     */
    public String registerDevice(String deviceName, String deviceType, String platform) {
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

            ApiResponse response = ApiResponse.builder()
                    .success(true)
                    .message("Device registered successfully")
                    .data(data)
                    .timestamp(LocalDateTime.now())
                    .build();

            return JsonUtil.toJson(response);
        } catch (Exception e) {
            log.error("Error registering device", e);

            ApiResponse response = ApiResponse.builder()
                    .success(false)
                    .message("Error registering device: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();

            return JsonUtil.toJson(response);
        }
    }

    /**
     * Update device information
     */
    public String updateDevice(String deviceId, String deviceName, String pushToken) {
        try {
            User currentUser = getCurrentUser();

            Long id = Long.parseLong(deviceId);

            Device device = deviceRepository.findById(id)
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

            ApiResponse response = ApiResponse.builder()
                    .success(true)
                    .message("Device updated successfully")
                    .timestamp(LocalDateTime.now())
                    .build();

            return JsonUtil.toJson(response);
        } catch (Exception e) {
            log.error("Error updating device", e);

            ApiResponse response = ApiResponse.builder()
                    .success(false)
                    .message("Error updating device: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();

            return JsonUtil.toJson(response);
        }
    }

    /**
     * Get device information
     */
    public String getDeviceInfo() {
        try {
            Map<String, Object> info = new HashMap<>();

            // Basic system information
            info.put("os", System.getProperty("os.name"));
            info.put("osVersion", System.getProperty("os.version"));
            info.put("javaVersion", System.getProperty("java.version"));
            info.put("availableProcessors", Runtime.getRuntime().availableProcessors());
            info.put("totalMemory", Runtime.getRuntime().totalMemory());
            info.put("freeMemory", Runtime.getRuntime().freeMemory());
            info.put("maxMemory", Runtime.getRuntime().maxMemory());
            info.put("timestamp", LocalDateTime.now().toString());

            return JsonUtil.toJson(info);
        } catch (Exception e) {
            log.error("Error getting device info", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get device info: " + e.getMessage());
            return JsonUtil.toJson(error);
        }
    }

    /**
     * Get network information
     */
    public String getNetworkInfo() {
        try {
            Map<String, Object> info = new HashMap<>();

            // This would be overridden in the native application
            info.put("online", true);
            info.put("networkType", "unknown");
            info.put("connectionQuality", "unknown");
            info.put("timestamp", LocalDateTime.now().toString());

            return JsonUtil.toJson(info);
        } catch (Exception e) {
            log.error("Error getting network info", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get network info: " + e.getMessage());
            return JsonUtil.toJson(error);
        }
    }

    /**
     * Check if this bridge is available in the current environment
     * @return true if available
     */
    public boolean isAvailable() {
        return true;
    }

    // Helper methods
    private User getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || authentication.getPrincipal() == null) {
                throw new RuntimeException("No authenticated user found");
            }

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            return userRepository.findById(userPrincipal.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
        } catch (Exception e) {
            log.error("Error getting current user", e);
            throw new RuntimeException("Error getting current user: " + e.getMessage());
        }
    }
}