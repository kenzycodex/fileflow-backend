package com.fileflow.dto.response.statistics;

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
public class SystemStatistics {
    private long totalUsers;
    private long activeUsers;
    private long totalFiles;
    private long totalFolders;
    private long totalStorageUsed;
    private long activitiesLast24Hours;
    private LocalDateTime timestamp;
}