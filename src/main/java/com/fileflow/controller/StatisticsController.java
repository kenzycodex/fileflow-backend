package com.fileflow.controller;

import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.service.statistics.StatisticsService;
import com.fileflow.service.websocket.WebSocketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for statistics endpoints
 */
@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
@Tag(name = "Statistics", description = "Statistics API")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final WebSocketService webSocketService;

    @GetMapping("/storage")
    @Operation(summary = "Get storage statistics")
    public ResponseEntity<ApiResponse> getStorageStatistics() {
        log.debug("Request to get storage statistics");
        return ResponseEntity.ok(statisticsService.getStorageStatistics());
    }

    @GetMapping("/activity")
    @Operation(summary = "Get activity statistics")
    public ResponseEntity<ApiResponse> getActivityStatistics(
            @Parameter(description = "Start date (ISO format: yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @Parameter(description = "End date (ISO format: yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.debug("Request to get activity statistics from {} to {}", startDate, endDate);
        return ResponseEntity.ok(statisticsService.getActivityStatistics(startDate, endDate));
    }

    @GetMapping("/file-types")
    @Operation(summary = "Get file type statistics")
    public ResponseEntity<ApiResponse> getFileTypeStatistics() {
        log.debug("Request to get file type statistics");
        return ResponseEntity.ok(statisticsService.getFileTypeStatistics());
    }

    @GetMapping("/sharing")
    @Operation(summary = "Get sharing statistics")
    public ResponseEntity<ApiResponse> getSharingStatistics() {
        log.debug("Request to get sharing statistics");
        return ResponseEntity.ok(statisticsService.getSharingStatistics());
    }

    @GetMapping("/system")
    @Operation(summary = "Get system statistics (admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getSystemStatistics() {
        log.debug("Request to get system statistics");
        return ResponseEntity.ok(statisticsService.getSystemStatistics());
    }

    @GetMapping("/websocket")
    @Operation(summary = "Get WebSocket statistics for current user")
    public ResponseEntity<ApiResponse> getWebSocketStatistics() {
        log.debug("Request to get WebSocket statistics");

        Map<String, Object> stats = webSocketService.getWebSocketStats();

        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("WebSocket statistics retrieved successfully")
                .data(stats)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/websocket/metrics")
    @Operation(summary = "Get WebSocket metrics for a date range (admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getWebSocketMetrics(
            @Parameter(description = "Start date (ISO format: yyyy-MM-dd)")
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().minusDays(30)}")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @Parameter(description = "End date (ISO format: yyyy-MM-dd)")
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now()}")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.debug("Request to get WebSocket metrics from {} to {}", startDate, endDate);

        Map<String, Object> metrics = webSocketService.getMetricsForDateRange(startDate, endDate);

        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("WebSocket metrics retrieved successfully")
                .data(metrics)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard overview statistics")
    public ResponseEntity<ApiResponse> getDashboardStatistics() {
        log.debug("Request to get dashboard statistics");

        // Get storage statistics
        ApiResponse storageStats = statisticsService.getStorageStatistics();

        // Get activity statistics for last 30 days
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        ApiResponse activityStats = statisticsService.getActivityStatistics(startDate, endDate);

        // Get file type statistics
        ApiResponse fileTypeStats = statisticsService.getFileTypeStatistics();

        // Get WebSocket stats
        Map<String, Object> webSocketStats = webSocketService.getWebSocketStats();

        // Combine into dashboard summary
        Map<String, Object> dashboardData = new HashMap<>();
        dashboardData.put("storage", storageStats.getData());
        dashboardData.put("activity", activityStats.getData());
        dashboardData.put("fileTypes", fileTypeStats.getData());
        dashboardData.put("websocket", webSocketStats);
        dashboardData.put("lastUpdated", LocalDateTime.now());

        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Dashboard statistics retrieved successfully")
                .data(dashboardData)
                .timestamp(LocalDateTime.now())
                .build());
    }
}