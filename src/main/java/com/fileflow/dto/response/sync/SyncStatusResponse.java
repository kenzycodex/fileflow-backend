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
public class SyncStatusResponse {
    private Long deviceId;
    private LocalDateTime lastSyncDate;
    private int pendingItemsCount;
    private LocalDateTime lastActiveTime;
}