package com.fileflow.repository;

import com.fileflow.model.WebSocketNotificationQueue;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for WebSocket notification queue
 */
@Repository
public interface WebSocketNotificationQueueRepository extends JpaRepository<WebSocketNotificationQueue, Long> {

    /**
     * Find unsent notifications for a user
     */
    List<WebSocketNotificationQueue> findByUser_IdAndIsSentFalseOrderByCreatedAt(Long userId);

    /**
     * Find unsent notifications for a user with limit
     */
    List<WebSocketNotificationQueue> findByUser_IdAndIsSentFalseOrderByCreatedAt(Long userId, Pageable pageable);

    /**
     * Count unsent notifications for a user
     */
    long countByUser_IdAndIsSentFalse(Long userId);

    /**
     * Find notifications to retry (not sent, has retries left, last retry before given time)
     */
    @Query("SELECT n FROM WebSocketNotificationQueue n WHERE n.isSent = false " +
            "AND n.retryCount < :maxRetries " +
            "AND (n.lastRetry IS NULL OR n.lastRetry < :cutoffTime)")
    List<WebSocketNotificationQueue> findNotificationsToRetry(
            @Param("maxRetries") int maxRetries,
            @Param("cutoffTime") LocalDateTime cutoffTime,
            Pageable pageable);

    /**
     * Mark notification as sent
     */
    @Modifying
    @Query("UPDATE WebSocketNotificationQueue n SET n.isSent = true, n.sentAt = :timestamp WHERE n.id = :id")
    void markAsSent(@Param("id") Long id, @Param("timestamp") LocalDateTime timestamp);

    /**
     * Update retry count
     */
    @Modifying
    @Query("UPDATE WebSocketNotificationQueue n SET n.retryCount = n.retryCount + 1, n.lastRetry = :timestamp WHERE n.id = :id")
    void incrementRetryCount(@Param("id") Long id, @Param("timestamp") LocalDateTime timestamp);

    /**
     * Delete old sent notifications
     */
    void deleteByIsSentTrueAndSentAtBefore(LocalDateTime timestamp);

    /**
     * Delete failed notifications that exceeded retry count
     */
    void deleteByIsSentFalseAndRetryCountGreaterThanEqualAndLastRetryBefore(
            int maxRetries, LocalDateTime timestamp);
}