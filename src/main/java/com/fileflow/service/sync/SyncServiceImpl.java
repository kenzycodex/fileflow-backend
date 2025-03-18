package com.fileflow.service.sync;

import com.fileflow.dto.request.sync.SyncBatchRequest;
import com.fileflow.dto.request.sync.SyncItemRequest;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.sync.SyncStatusResponse;
import com.fileflow.exception.BadRequestException;
import com.fileflow.exception.ResourceNotFoundException;
import com.fileflow.model.Device;
import com.fileflow.model.SyncQueue;
import com.fileflow.model.User;
import com.fileflow.repository.DeviceRepository;
import com.fileflow.repository.SyncQueueRepository;
import com.fileflow.repository.UserRepository;
import com.fileflow.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncServiceImpl implements SyncService {

    private final DeviceRepository deviceRepository;
    private final SyncQueueRepository syncQueueRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Device registerDevice(String deviceName, String deviceType, String platform) {
        User currentUser = getCurrentUser();

        // Check if device already registered
        List<Device> existingDevices = deviceRepository.findByUserAndDeviceNameAndDeviceTypeAndPlatform(
                currentUser, deviceName, deviceType, platform);

        if (!existingDevices.isEmpty()) {
            Device existingDevice = existingDevices.get(0);
            existingDevice.setLastActive(LocalDateTime.now());
            return deviceRepository.save(existingDevice);
        }

        // Create new device
        Device device = Device.builder()
                .user(currentUser)
                .deviceName(deviceName)
                .deviceType(deviceType)
                .platform(platform)
                .lastSyncDate(null)
                .pushToken(null)
                .createdAt(LocalDateTime.now())
                .lastActive(LocalDateTime.now())
                .build();

        return deviceRepository.save(device);
    }

    @Override
    @Transactional
    public Device updateDevicePushToken(Long deviceId, String pushToken) {
        User currentUser = getCurrentUser();

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device", "id", deviceId));

        // Check if device belongs to current user
        if (!device.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Device does not belong to current user");
        }

        device.setPushToken(pushToken);
        device.setLastActive(LocalDateTime.now());

        return deviceRepository.save(device);
    }

    @Override
    public SyncStatusResponse getSyncStatus(Long deviceId) {
        User currentUser = getCurrentUser();

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device", "id", deviceId));

        // Check if device belongs to current user
        if (!device.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Device does not belong to current user");
        }

        // Get pending sync items
        List<SyncQueue> pendingItems = syncQueueRepository.findByDeviceIdAndStatus(
                deviceId, SyncQueue.Status.PENDING);

        // Update device last active time
        device.setLastActive(LocalDateTime.now());
        deviceRepository.save(device);

        return SyncStatusResponse.builder()
                .deviceId(deviceId)
                .lastSyncDate(device.getLastSyncDate())
                .pendingItemsCount(pendingItems.size())
                .lastActiveTime(device.getLastActive())
                .build();
    }

    @Override
    @Transactional
    public ApiResponse addToSyncQueue(Long deviceId, SyncItemRequest request) {
        User currentUser = getCurrentUser();

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device", "id", deviceId));

        // Check if device belongs to current user
        if (!device.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Device does not belong to current user");
        }

        // Create sync queue item
        SyncQueue syncItem = SyncQueue.builder()
                .userId(currentUser.getId())
                .deviceId(deviceId)
                .actionType(request.getActionType())
                .itemId(request.getItemId())
                .itemType(request.getItemType())
                .status(SyncQueue.Status.PENDING)
                .created_at(LocalDateTime.now())
                .processedAt(null)
                .retryCount(0)
                .dataPayload(request.getDataPayload())
                .build();

        syncQueueRepository.save(syncItem);

        return ApiResponse.builder()
                .success(true)
                .message("Item added to sync queue")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public List<SyncQueue> getPendingSyncItems(Long deviceId) {
        User currentUser = getCurrentUser();

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device", "id", deviceId));

        // Check if device belongs to current user
        if (!device.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Device does not belong to current user");
        }

        return syncQueueRepository.findByDeviceIdAndStatus(deviceId, SyncQueue.Status.PENDING);
    }

    @Override
    @Transactional
    public SyncStatusResponse processSyncBatch(Long deviceId, SyncBatchRequest request) {
        User currentUser = getCurrentUser();

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device", "id", deviceId));

        // Check if device belongs to current user
        if (!device.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Device does not belong to current user");
        }

        // Process successful items
        if (request.getSuccessfulItems() != null && !request.getSuccessfulItems().isEmpty()) {
            List<SyncQueue> successfulItems = syncQueueRepository.findAllById(request.getSuccessfulItems());

            for (SyncQueue item : successfulItems) {
                item.setStatus(SyncQueue.Status.COMPLETED);
                item.setProcessedAt(LocalDateTime.now());
            }

            syncQueueRepository.saveAll(successfulItems);
        }

        // Process failed items
        if (request.getFailedItems() != null && !request.getFailedItems().isEmpty()) {
            List<SyncQueue> failedItems = syncQueueRepository.findAllById(request.getFailedItems());

            for (SyncQueue item : failedItems) {
                item.setRetryCount(item.getRetryCount() + 1);

                // If retry count exceeds maximum, mark as failed
                if (item.getRetryCount() >= 3) {
                    item.setStatus(SyncQueue.Status.FAILED);
                    item.setProcessedAt(LocalDateTime.now());
                }
            }

            syncQueueRepository.saveAll(failedItems);
        }

        // Update device sync status
        device.setLastSyncDate(LocalDateTime.now());
        device.setLastActive(LocalDateTime.now());
        deviceRepository.save(device);

        // Get remaining pending items
        List<SyncQueue> pendingItems = syncQueueRepository.findByDeviceIdAndStatus(
                deviceId, SyncQueue.Status.PENDING);

        return SyncStatusResponse.builder()
                .deviceId(deviceId)
                .lastSyncDate(device.getLastSyncDate())
                .pendingItemsCount(pendingItems.size())
                .lastActiveTime(device.getLastActive())
                .build();
    }

    @Override
    @Transactional
    public ApiResponse markSyncItemProcessed(Long queueId, boolean success) {
        User currentUser = getCurrentUser();

        SyncQueue syncItem = syncQueueRepository.findById(queueId)
                .orElseThrow(() -> new ResourceNotFoundException("Sync queue item", "id", queueId));

        // Check if item belongs to current user
        if (!syncItem.getUserId().equals(currentUser.getId())) {
            throw new BadRequestException("Sync queue item does not belong to current user");
        }

        if (success) {
            syncItem.setStatus(SyncQueue.Status.COMPLETED);
        } else {
            syncItem.setRetryCount(syncItem.getRetryCount() + 1);

            // If retry count exceeds maximum, mark as failed
            if (syncItem.getRetryCount() >= 3) {
                syncItem.setStatus(SyncQueue.Status.FAILED);
            }
        }

        syncItem.setProcessedAt(LocalDateTime.now());
        syncQueueRepository.save(syncItem);

        return ApiResponse.builder()
                .success(true)
                .message("Sync item marked as " + (success ? "completed" : "failed"))
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public int cleanupProcessedSyncItems(int hoursToKeep) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hoursToKeep);

        List<SyncQueue> itemsToDelete = syncQueueRepository.findByStatusInAndProcessedAtBefore(
                List.of(SyncQueue.Status.COMPLETED, SyncQueue.Status.FAILED),
                cutoffTime);

        if (!itemsToDelete.isEmpty()) {
            syncQueueRepository.deleteAll(itemsToDelete);
            log.info("Deleted {} processed sync items", itemsToDelete.size());
            return itemsToDelete.size();
        }

        return 0;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}