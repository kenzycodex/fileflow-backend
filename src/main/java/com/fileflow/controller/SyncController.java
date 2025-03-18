package com.fileflow.controller;

import com.fileflow.dto.request.sync.DeviceRegistrationRequest;
import com.fileflow.dto.request.sync.PushTokenUpdateRequest;
import com.fileflow.dto.request.sync.SyncBatchRequest;
import com.fileflow.dto.request.sync.SyncItemRequest;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.sync.DeviceResponse;
import com.fileflow.dto.response.sync.SyncStatusResponse;
import com.fileflow.model.Device;
import com.fileflow.model.SyncQueue;
import com.fileflow.service.sync.SyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
@Tag(name = "Synchronization", description = "Synchronization API for offline devices")
@SecurityRequirement(name = "bearerAuth")
public class SyncController {

    private final SyncService syncService;

    @PostMapping("/devices")
    @Operation(summary = "Register a device for sync")
    public ResponseEntity<DeviceResponse> registerDevice(@Valid @RequestBody DeviceRegistrationRequest request) {
        Device device = syncService.registerDevice(
                request.getDeviceName(),
                request.getDeviceType(),
                request.getPlatform());

        return ResponseEntity.status(HttpStatus.CREATED).body(mapDeviceToDeviceResponse(device));
    }

    @PutMapping("/devices/{deviceId}/token")
    @Operation(summary = "Update device push token")
    public ResponseEntity<DeviceResponse> updatePushToken(
            @PathVariable Long deviceId,
            @Valid @RequestBody PushTokenUpdateRequest request) {
        Device device = syncService.updateDevicePushToken(deviceId, request.getPushToken());
        return ResponseEntity.ok(mapDeviceToDeviceResponse(device));
    }

    @GetMapping("/status/{deviceId}")
    @Operation(summary = "Get sync status for device")
    public ResponseEntity<SyncStatusResponse> getSyncStatus(@PathVariable Long deviceId) {
        return ResponseEntity.ok(syncService.getSyncStatus(deviceId));
    }

    @PostMapping("/queue/{deviceId}")
    @Operation(summary = "Add item to sync queue")
    public ResponseEntity<ApiResponse> addToSyncQueue(
            @PathVariable Long deviceId,
            @Valid @RequestBody SyncItemRequest request) {
        return ResponseEntity.ok(syncService.addToSyncQueue(deviceId, request));
    }

    @GetMapping("/queue/{deviceId}")
    @Operation(summary = "Get pending sync items for device")
    public ResponseEntity<List<SyncItemRequest>> getPendingSyncItems(@PathVariable Long deviceId) {
        List<SyncQueue> syncItems = syncService.getPendingSyncItems(deviceId);

        List<SyncItemRequest> response = syncItems.stream()
                .map(item -> SyncItemRequest.builder()
                        .id(item.getId())
                        .actionType(item.getActionType())
                        .itemId(item.getItemId())
                        .itemType(item.getItemType())
                        .dataPayload(item.getDataPayload())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/process/{deviceId}")
    @Operation(summary = "Process sync batch")
    public ResponseEntity<SyncStatusResponse> processSyncBatch(
            @PathVariable Long deviceId,
            @Valid @RequestBody SyncBatchRequest request) {
        return ResponseEntity.ok(syncService.processSyncBatch(deviceId, request));
    }

    @PostMapping("/queue/{queueId}/complete")
    @Operation(summary = "Mark sync item as processed")
    public ResponseEntity<ApiResponse> markSyncItemProcessed(
            @PathVariable Long queueId,
            @RequestParam(defaultValue = "true") boolean success) {
        return ResponseEntity.ok(syncService.markSyncItemProcessed(queueId, success));
    }

    private DeviceResponse mapDeviceToDeviceResponse(Device device) {
        return DeviceResponse.builder()
                .id(device.getId())
                .deviceName(device.getDeviceName())
                .deviceType(device.getDeviceType())
                .platform(device.getPlatform())
                .lastSyncDate(device.getLastSyncDate())
                .lastActive(device.getLastActive())
                .createdAt(device.getCreatedAt())
                .build();
    }
}
