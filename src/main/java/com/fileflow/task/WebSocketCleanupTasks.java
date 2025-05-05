package com.fileflow.task;

import com.fileflow.repository.WebSocketMetricsRepository;
import com.fileflow.repository.WebSocketNotificationQueueRepository;
import com.fileflow.repository.WebSocketSessionRepository;
import com.fileflow.repository.WebSocketSubscriptionRepository;
import com.fileflow.service.websocket.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled tasks for WebSocket maintenance
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketCleanupTasks {

    private final WebSocketSessionRepository sessionRepository;
    private final WebSocketSubscriptionRepository subscriptionRepository;
    private final WebSocketNotificationQueueRepository notificationQueueRepository;
    private final WebSocketMetricsRepository metricsRepository;
    private final WebSocketService webSocketService;

    // Maximum retry attempts for notifications
    private static final int MAX_RETRY_ATTEMPTS = 5;
    // Time to keep metrics (days)
    private static final int METRICS_RETENTION_DAYS = 90;
    // Time to keep notification history (days)
    private static final int NOTIFICATION_HISTORY_DAYS = 30;
    // Time to consider a session stale (minutes)
    private static final int SESSION_TIMEOUT_MINUTES = 30;
    // Batch size for processing
    private static final int BATCH_SIZE = 100;

    /**
     * Clean up stale WebSocket sessions
     * Runs every hour
     */
    @Scheduled(cron = "0 0 * * * ?")
    @Transactional
    public void cleanupStaleSessions() {
        log.info("Starting cleanup of stale WebSocket sessions");

        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(SESSION_TIMEOUT_MINUTES);
            List<com.fileflow.model.WebSocketSession> staleSessions =
                    sessionRepository.findByIsActiveTrueAndLastActivityBefore(cutoffTime);

            for (com.fileflow.model.WebSocketSession session : staleSessions) {
                log.info("Marking stale session as disconnected: {}", session.getSessionId());
                sessionRepository.markAsDisconnected(session.getSessionId(), LocalDateTime.now());
            }

            log.info("Completed cleanup of stale WebSocket sessions. Processed {} sessions.", staleSessions.size());
        } catch (Exception e) {
            log.error("Error during stale session cleanup", e);
        }
    }

    /**
     * Clean up old WebSocket sessions
     * Runs daily at 1 AM
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void cleanupOldSessions() {
        log.info("Starting cleanup of old WebSocket sessions");

        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(7);
            List<com.fileflow.model.WebSocketSession> oldSessions =
                    sessionRepository.findByIsActiveFalseAndDisconnectedAtBefore(cutoffTime);

            sessionRepository.deleteAll(oldSessions);

            log.info("Completed cleanup of old WebSocket sessions. Deleted {} sessions.", oldSessions.size());
        } catch (Exception e) {
            log.error("Error during old session cleanup", e);
        }
    }

    /**
     * Clean up inactive subscriptions
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupInactiveSubscriptions() {
        log.info("Starting cleanup of inactive WebSocket subscriptions");

        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(30);
            subscriptionRepository.deleteByIsActiveFalseAndUpdatedAtBefore(cutoffTime);

            log.info("Completed cleanup of inactive WebSocket subscriptions");
        } catch (Exception e) {
            log.error("Error during inactive subscription cleanup", e);
        }
    }

    /**
     * Clean up old notification queues
     * Runs daily at 3 AM
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupNotificationQueue() {
        log.info("Starting cleanup of notification queue");

        try {
            // Delete old sent notifications
            LocalDateTime sentCutoffTime = LocalDateTime.now().minusDays(NOTIFICATION_HISTORY_DAYS);
            notificationQueueRepository.deleteByIsSentTrueAndSentAtBefore(sentCutoffTime);

            // Delete failed notifications that exceeded retry count
            LocalDateTime failedCutoffTime = LocalDateTime.now().minusDays(7);
            notificationQueueRepository.deleteByIsSentFalseAndRetryCountGreaterThanEqualAndLastRetryBefore(
                    MAX_RETRY_ATTEMPTS, failedCutoffTime);

            log.info("Completed cleanup of notification queue");
        } catch (Exception e) {
            log.error("Error during notification queue cleanup", e);
        }
    }

    /**
     * Clean up old metrics
     * Runs monthly on the 1st at 4 AM
     */
    @Scheduled(cron = "0 0 4 1 * ?")
    @Transactional
    public void cleanupOldMetrics() {
        log.info("Starting cleanup of old WebSocket metrics");

        try {
            LocalDate cutoffDate = LocalDate.now().minusDays(METRICS_RETENTION_DAYS);
            metricsRepository.deleteByEventDateBefore(cutoffDate);

            log.info("Completed cleanup of old WebSocket metrics");
        } catch (Exception e) {
            log.error("Error during metrics cleanup", e);
        }
    }

    /**
     * Process notification retry queue
     * Runs every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void processNotificationRetryQueue() {
        log.debug("Starting processing of notification retry queue");

        try {
            webSocketService.processRetryNotifications();
            log.debug("Completed processing of notification retry queue");
        } catch (Exception e) {
            log.error("Error during notification retry processing", e);
        }
    }

    /**
     * Process notification queues for connected users
     * Runs every minute
     */
    @Scheduled(fixedRate = 60000) // 1 minute
    public void processNotificationQueues() {
        log.debug("Starting processing of notification queues for connected users");

        try {
            // Get all active sessions (limit to 100 to avoid processing too many at once)
            List<com.fileflow.model.WebSocketSession> activeSessions =
                    sessionRepository.findAll(PageRequest.of(0, 100)).getContent().stream()
                            .filter(com.fileflow.model.WebSocketSession::isActive)
                            .toList();

            int totalProcessed = 0;

            // Process for each connected user
            for (com.fileflow.model.WebSocketSession session : activeSessions) {
                int processed = webSocketService.processQueuedNotifications(session.getUser().getId());
                totalProcessed += processed;
            }

            if (totalProcessed > 0) {
                log.info("Processed {} queued notifications for connected users", totalProcessed);
            }
        } catch (Exception e) {
            log.error("Error during notification queue processing", e);
        }
    }
}