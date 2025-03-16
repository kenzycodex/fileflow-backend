package com.fileflow.service.device;

import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.common.PagedResponse;
import com.fileflow.exception.BadRequestException;
import com.fileflow.exception.ResourceNotFoundException;
import com.fileflow.model.Device;
import com.fileflow.model.User;
import com.fileflow.repository.DeviceRepository;
import com.fileflow.repository.UserRepository;
import com.fileflow.security.UserPrincipal;
import com.fileflow.service.activity.ActivityService;
import com.fileflow.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final ActivityService activityService;

    @Override
    public PagedResponse<Device> getUserDevices(int page, int size) {
        User currentUser = getCurrentUser();
        validatePageNumberAndSize(page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "lastActive");
        Page<Device> devices = deviceRepository.findByUser(currentUser, pageable);

        if (devices.getNumberOfElements() == 0) {
            return new PagedResponse<>(Collections.emptyList(), devices.getNumber(),
                    devices.getSize(), devices.getTotalElements(), devices.getTotalPages(), devices.isLast());
        }

        return new PagedResponse<>(devices.getContent(), devices.getNumber(),
                devices.getSize(), devices.getTotalElements(), devices.getTotalPages(), devices.isLast());
    }

    @Override
    @Transactional
    public Device registerDevice(String deviceName, String deviceType, String platform, String pushToken) {
        if (deviceName == null || deviceName.trim().isEmpty()) {
            throw new BadRequestException("Device name cannot be empty");
        }

        if (deviceType == null || deviceType.trim().isEmpty()) {
            throw new BadRequestException("Device type cannot be empty");
        }

        if (platform == null || platform.trim().isEmpty()) {
            throw new BadRequestException("Platform cannot be empty");
        }

        User currentUser = getCurrentUser();

        // Check if device already exists by name
        if (deviceRepository.existsByUserAndDeviceName(currentUser, deviceName)) {
            throw new BadRequestException("Device with name '" + deviceName + "' already exists");
        }

        // Create device
        Device device = Device.builder()
                .user(currentUser)
                .deviceName(deviceName)
                .deviceType(deviceType)
                .platform(platform)
                .pushToken(pushToken)
                .lastSyncDate(LocalDateTime.now())
                .lastActive(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        Device savedDevice = deviceRepository.save(device);

        // Log activity
        activityService.logActivity(
                currentUser.getId(),
                Constants.ACTIVITY_DEVICE_REGISTER,
                Constants.ITEM_TYPE_DEVICE,
                savedDevice.getId(),
                "Registered device: " + savedDevice.getDeviceName() + " (" + savedDevice.getDeviceType() + ")"
        );

        return savedDevice;
    }

    @Override
    @Transactional
    public Device updateDevice(Long deviceId, String deviceName, String pushToken) {
        User currentUser = getCurrentUser();

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device", "id", deviceId));

        // Ensure device belongs to current user
        if (!device.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Device not found or does not belong to you");
        }

        // Update name if provided
        if (deviceName != null && !deviceName.trim().isEmpty() && !deviceName.equals(device.getDeviceName())) {
            // Check for duplicates
            if (deviceRepository.existsByUserAndDeviceNameAndIdNot(currentUser, deviceName, deviceId)) {
                throw new BadRequestException("Device with name '" + deviceName + "' already exists");
            }

            device.setDeviceName(deviceName);
        }

        // Update push token if provided
        if (pushToken != null) {
            device.setPushToken(pushToken);
        }

        device.setLastActive(LocalDateTime.now());

        Device updatedDevice = deviceRepository.save(device);

        // Log activity
        activityService.logActivity(
                currentUser.getId(),
                Constants.ACTIVITY_DEVICE_UPDATE,
                Constants.ITEM_TYPE_DEVICE,
                updatedDevice.getId(),
                "Updated device: " + updatedDevice.getDeviceName()
        );

        return updatedDevice;
    }

    @Override
    @Transactional
    public ApiResponse deleteDevice(Long deviceId) {
        User currentUser = getCurrentUser();

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device", "id", deviceId));

        // Ensure device belongs to current user
        if (!device.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Device not found or does not belong to you");
        }

        String deviceName = device.getDeviceName();

        // Delete device
        deviceRepository.delete(device);

        // Log activity
        activityService.logActivity(
                currentUser.getId(),
                Constants.ACTIVITY_DEVICE_DELETE,
                Constants.ITEM_TYPE_DEVICE,
                null,
                "Deleted device: " + deviceName
        );

        return ApiResponse.builder()
                .success(true)
                .message("Device deleted successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public ApiResponse syncDevice(Long deviceId) {
        User currentUser = getCurrentUser();

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device", "id", deviceId));

        // Ensure device belongs to current user
        if (!device.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Device not found or does not belong to you");
        }

        // Update sync date
        device.setLastSyncDate(LocalDateTime.now());
        device.setLastActive(LocalDateTime.now());
        deviceRepository.save(device);

        // Log activity
        activityService.logActivity(
                currentUser.getId(),
                Constants.ACTIVITY_DEVICE_SYNC,
                Constants.ITEM_TYPE_DEVICE,
                device.getId(),
                "Synced device: " + device.getDeviceName()
        );

        return ApiResponse.builder()
                .success(true)
                .message("Device synced successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public Device getDevice(Long deviceId) {
        User currentUser = getCurrentUser();

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device", "id", deviceId));

        // Ensure device belongs to current user
        if (!device.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Device not found or does not belong to you");
        }

        // Update last active
        device.setLastActive(LocalDateTime.now());
        return deviceRepository.save(device);
    }

    // Helper methods

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void validatePageNumberAndSize(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("Page number cannot be less than zero.");
        }

        if (size < 1) {
            throw new BadRequestException("Page size must not be less than one.");
        }

        if (size > 100) {
            throw new BadRequestException("Page size must not be greater than 100.");
        }
    }
}