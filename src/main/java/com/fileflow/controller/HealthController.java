package com.fileflow.controller;

import com.fileflow.dto.response.health.HealthCheckResponse;
import com.fileflow.service.health.HealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
@Tag(name = "System Health", description = "Endpoint for system health checks")
public class HealthController {

    private final HealthService healthService;

    @GetMapping
    @Operation(
            summary = "Get system health status",
            description = "Provides comprehensive health check for all system components",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful health check",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = HealthCheckResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "503",
                            description = "Service Unavailable - One or more components are down",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = HealthCheckResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<HealthCheckResponse> checkHealth() {
        HealthCheckResponse healthCheck = healthService.checkHealth();

        // Determine HTTP status based on overall system status
        HttpStatus status = healthCheck.getStatus().equals("UP")
                ? HttpStatus.OK
                : HttpStatus.SERVICE_UNAVAILABLE;

        return ResponseEntity
                .status(status)
                .body(healthCheck);
    }

    @GetMapping("/database")
    @Operation(
            summary = "Check database health",
            description = "Provides detailed health status of the database component"
    )
    public ResponseEntity<Object> checkDatabaseHealth() {
        return ResponseEntity.ok(healthService.checkDatabase());
    }

    @GetMapping("/storage")
    @Operation(
            summary = "Check storage service health",
            description = "Provides detailed health status of the storage service"
    )
    public ResponseEntity<Object> checkStorageHealth() {
        return ResponseEntity.ok(healthService.checkStorage());
    }

    @GetMapping("/email")
    @Operation(
            summary = "Check email service health",
            description = "Provides detailed health status of the email service"
    )
    public ResponseEntity<Object> checkEmailHealth() {
        return ResponseEntity.ok(healthService.checkEmail());
    }

    @GetMapping("/metrics")
    @Operation(
            summary = "Get system metrics",
            description = "Retrieves current system performance metrics"
    )
    public ResponseEntity<Object> getSystemMetrics() {
        return ResponseEntity.ok(healthService.getSystemMetrics());
    }
}