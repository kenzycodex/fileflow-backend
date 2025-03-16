package com.fileflow.dto.response.statistics;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StorageStatistics {
    private Long userId;
    private String username;
    private long totalQuota;
    private long usedStorage;
    private long availableStorage;
    private double usagePercentage;
    private long fileCount;
    private long folderCount;
    private long deletedFileCount;
    private long deletedFolderCount;
    private double averageFileSize;
}