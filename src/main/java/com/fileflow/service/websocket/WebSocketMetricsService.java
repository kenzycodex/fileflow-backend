package com.fileflow.service.websocket;

import java.time.LocalDate;
import java.util.Map;

/**
 * Service for WebSocket metrics operations
 */
public interface WebSocketMetricsService {

    /**
     * Get metrics for a date range
     */
    Map<String, Object> getMetricsForDateRange(LocalDate startDate, LocalDate endDate);

    /**
     * Get WebSocket stats for the current user
     */
    Map<String, Object> getWebSocketStats();

    /**
     * Record WebSocket metrics
     */
    void recordMetrics(int messagesReceived, int messagesSent, int errors, int averageMessageSize);

    /**
     * Count active WebSocket connections
     */
    int countActiveConnections();
}