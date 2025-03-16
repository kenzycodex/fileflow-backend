package com.fileflow.controller;

import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.service.statistics.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
@Tag(name = "Statistics", description = "Statistics API")
@SecurityRequirement(name = "bearerAuth")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/storage")
    @Operation(summary = "Get storage statistics")
    public ResponseEntity<ApiResponse> getStorageStatistics() {
        return ResponseEntity.ok(statisticsService.getStorageStatistics());
    }

    @GetMapping("/activity")
    @Operation(summary = "Get activity statistics")
    public ResponseEntity<ApiResponse> getActivityStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(statisticsService.getActivityStatistics(startDate, endDate));
    }

    @GetMapping("/file-types")
    @Operation(summary = "Get file type statistics")
    public ResponseEntity<ApiResponse> getFileTypeStatistics() {
        return ResponseEntity.ok(statisticsService.getFileTypeStatistics());
    }

    @GetMapping("/sharing")
    @Operation(summary = "Get sharing statistics")
    public ResponseEntity<ApiResponse> getSharingStatistics() {
        return ResponseEntity.ok(statisticsService.getSharingStatistics());
    }

    @GetMapping("/system")
    @Operation(summary = "Get system statistics (admin only)")
    public ResponseEntity<ApiResponse> getSystemStatistics() {
        return ResponseEntity.ok(statisticsService.getSystemStatistics());
    }
}