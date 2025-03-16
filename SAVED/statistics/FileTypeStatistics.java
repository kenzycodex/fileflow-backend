package com.fileflow.dto.response.statistics;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileTypeStatistics {
    private Long userId;
    private String username;
    private long totalFiles;
    private Map<String, Long> fileCountsByType;
    private Map<String, Long> storageByType;
}