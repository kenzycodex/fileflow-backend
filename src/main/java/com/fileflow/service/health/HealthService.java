package com.fileflow.service.health;

import com.fileflow.dto.response.health.HealthCheckResponse;
import java.util.Map;

public interface HealthService {
    /**
     * Perform health check on all system components
     *
     * @return health check response with overall system status
     */
    HealthCheckResponse checkHealth();

    /**
     * Check database connectivity
     *
     * @return database component status
     */
    Map<String, Object> checkDatabase();

    /**
     * Check storage service
     *
     * @return storage component status
     */
    Map<String, Object> checkStorage();

    /**
     * Check email service
     *
     * @return email component status
     */
    Map<String, Object> checkEmail();

    /**
     * Get system metrics
     *
     * @return system metrics
     */
    Map<String, Object> getSystemMetrics();
}