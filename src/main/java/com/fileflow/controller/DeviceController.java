package com.fileflow.controller;

import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.common.PagedResponse;
import com.fileflow.model.Device;
import com.fileflow.service.device.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
@Tag(name = "Device Management", description = "Device management API")
@SecurityRequirement(name = "bearerAuth")
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping
    @Operation(summary = "Get user devices")
    public ResponseEntity<PagedResponse<Device>> getUserDevices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(deviceService.getUserDevices(page, size));
    }

    @PostMapping
    @Operation(summary = "Register new device")
    public ResponseEntity<Device> registerDevice(
            @RequestParam @NotBlank String deviceName,
            @RequestParam @NotBlank String deviceType,
            @RequestParam @NotBlank String platform,
            @RequestParam(required = false) String pushToken) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(deviceService.registerDevice(deviceName, deviceType, platform, pushToken));
    }

    @PutMapping("/{deviceId}")
    @Operation(summary = "Update device")
    public ResponseEntity<Device> updateDevice(
            @PathVariable Long deviceId,
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) String pushToken) {
        return ResponseEntity.ok(deviceService.updateDevice(deviceId, deviceName, pushToken));
    }

    @DeleteMapping("/{deviceId}")
    @Operation(summary = "Delete device")
    public ResponseEntity<ApiResponse> deleteDevice(@PathVariable Long deviceId) {
        return ResponseEntity.ok(deviceService.deleteDevice(deviceId));
    }

    @PostMapping("/{deviceId}/sync")
    @Operation(summary = "Sync device")
    public ResponseEntity<ApiResponse> syncDevice(@PathVariable Long deviceId) {
        return ResponseEntity.ok(deviceService.syncDevice(deviceId));
    }

    @GetMapping("/{deviceId}")
    @Operation(summary = "Get device details")
    public ResponseEntity<Device> getDevice(@PathVariable Long deviceId) {
        return ResponseEntity.ok(deviceService.getDevice(deviceId));
    }
}
