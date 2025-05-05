package com.fileflow.repository;

import com.fileflow.model.WebSocketMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository for WebSocket metrics
 */
@Repository
public interface WebSocketMetricsRepository extends JpaRepository<WebSocketMetrics, Long> {

    /**
     * Find metrics for a specific date and hour
     */
    Optional<WebSocketMetrics> findByEventDateAndHourOfDay(LocalDate eventDate, int hourOfDay);

    /**
     * Find metrics for a date range
     */
    List<WebSocketMetrics> findByEventDateBetweenOrderByEventDateAscHourOfDayAsc(
            LocalDate startDate, LocalDate endDate);

    /**
     * Get total metrics for a date range
     */
    @Query("SELECT new map(" +
            "SUM(m.activeConnections) as totalConnections, " +
            "SUM(m.messagesSent) as totalMessagesSent, " +
            "SUM(m.messagesReceived) as totalMessagesReceived, " +
            "SUM(m.errorsCount) as totalErrors, " +
            "AVG(m.averageMessageSize) as avgMessageSize) " +
            "FROM WebSocketMetrics m WHERE m.eventDate BETWEEN :startDate AND :endDate")
    Map<String, Object> getTotalMetricsForDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Delete metrics older than a given date
     */
    void deleteByEventDateBefore(LocalDate date);
}