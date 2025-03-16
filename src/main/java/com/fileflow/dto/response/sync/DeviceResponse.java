package com.fileflow.dto.response.sync;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeviceResponse {
    private Long id;
    private String deviceName;
    private String deviceType;
    private String platform;
    private LocalDateTime lastSyncDate;
    private LocalDateTime lastActive;
    private LocalDateTime createdAt;
}