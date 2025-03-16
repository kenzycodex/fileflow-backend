package com.fileflow.dto.request.sync;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRegistrationRequest {
    @NotBlank(message = "Device name is required")
    @Size(max = 100, message = "Device name cannot exceed 100 characters")
    private String deviceName;

    @NotBlank(message = "Device type is required")
    @Size(max = 50, message = "Device type cannot exceed 50 characters")
    private String deviceType;

    @NotBlank(message = "Platform is required")
    @Size(max = 50, message = "Platform cannot exceed 50 characters")
    private String platform;
}
