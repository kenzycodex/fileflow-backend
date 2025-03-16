package com.fileflow.service.health;

import com.fileflow.dto.response.health.HealthCheckResponse;

import java.util.Map;

public interface HealthService {
    /**
     * Perform health check on all system components
     *
     * @return health check response
     */
    HealthCheckResponse checkHealth();

    /**
     * Check database connectivity
     *
     * @return component status
     */
    Map<String, Object> checkDatabase();

    /**
     * Check storage service
     *
     * @return component status
     */
    Map<String, Object> checkStorage();

    /**
     * Check email service
     *
     * @return component status
     */
    Map<String, Object> checkEmail();

    /**
     * Get system metrics
     *
     * @return system metrics
     */
    Map<String, Object> getSystemMetrics();
}