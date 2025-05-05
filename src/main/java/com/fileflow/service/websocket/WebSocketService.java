package com.fileflow.service.websocket;

import com.fileflow.dto.response.file.FileResponse;
import com.fileflow.model.User;
import com.fileflow.model.WebSocketMetrics;
import com.fileflow.model.WebSocketNotificationQueue;
import com.fileflow.model.WebSocketSubscription;
import com.fileflow.repository.*;
import com.fileflow.security.UserPrincipal;
import com.fileflow.util.JsonUtil;
import com.fileflow.websocket.WebSocketActionType;
import com.fileflow.websocket.WebSocketEventPublisher;
import com.fileflow.websocket.WebSocketMessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comprehensive service for handling WebSocket notifications and interactions
 * with enhanced persistence, metrics, and offline message support
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WebSocketService {

    private final WebSocketEventPublisher eventPublisher;
    private final WebSocketSessionRepository sessionRepository;
    private final WebSocketSubscriptionRepository subscriptionRepository;
    private final WebSocketNotificationQueueRepository notificationQueueRepository;
    private final WebSocketMetricsRepository metricsRepository;
    private final UserRepository userRepository;

    // Constants
    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final int BATCH_SIZE = 100;

    // Cache for user connection status to reduce DB queries
    private final Map<Long, Boolean> userConnectionCache = new ConcurrentHashMap<>();

    /**
     * Notify about file upload
     */
    @Transactional
    public void notifyFileUpload(FileResponse fileResponse) {
        log.debug("Notifying file upload: fileId={}, fileName={}", fileResponse.getId(), fileResponse.getFilename());

        try {
            Map<String, Object> data = new HashMap<>();
            data.put("file", fileResponse);
            data.put("ownerId", fileResponse.getOwnerId());

            // Publish event to connected users
            boolean delivered = publishFileEvent(fileResponse.getId(), WebSocketActionType.UPLOADED, data);

            // If not delivered to all subscribers, queue notifications
            if (!delivered) {
                queueFileNotification(fileResponse.getId(), WebSocketActionType.UPLOADED, data);
            }

            // Also notify about folder changes if file is in a folder
            if (fileResponse.getParentFolderId() != null) {
                Map<String, Object> folderData = new HashMap<>();
                folderData.put("folderId", fileResponse.getParentFolderId());
                folderData.put("fileId", fileResponse.getId());
                folderData.put("fileName", fileResponse.getFilename());
                folderData.put("ownerId", fileResponse.getOwnerId());

                // Publish folder event to connected users
                boolean folderDelivered = publishFolderEvent(
                        fileResponse.getParentFolderId(), WebSocketActionType.UPDATED, folderData);

                // If not delivered to all subscribers, queue notifications
                if (!folderDelivered) {
                    queueFolderNotification(fileResponse.getParentFolderId(), WebSocketActionType.UPDATED, folderData);
                }
            }

            // Record metrics for event
            recordMetric(0, 1, 0);
        } catch (Exception e) {
            log.error("Error notifying file upload: {}", e.getMessage(), e);
            recordMetric(0, 0, 1);
        }
    }

    /**
     * Notify about file update
     */
    @Transactional
    public void notifyFileUpdate(FileResponse fileResponse) {
        log.debug("Notifying file update: fileId={}, fileName={}", fileResponse.getId(), fileResponse.getFilename());

        try {
            Map<String, Object> data = new HashMap<>();
            data.put("file", fileResponse);
            data.put("ownerId", fileResponse.getOwnerId());

            // Publish event to connected users
            boolean delivered = publishFileEvent(fileResponse.getId(), WebSocketActionType.UPDATED, data);

            // If not delivered to all subscribers, queue notifications
            if (!delivered) {
                queueFileNotification(fileResponse.getId(), WebSocketActionType.UPDATED, data);
            }

            // Record metrics for event
            recordMetric(0, 1, 0);
        } catch (Exception e) {
            log.error("Error notifying file update: {}", e.getMessage(), e);
            recordMetric(0, 0, 1);
        }
    }

    /**
     * Notify about file deletion
     */
    @Transactional
    public void notifyFileDelete(Long fileId, Long ownerId, String fileName, Long parentFolderId) {
        log.debug("Notifying file deletion: fileId={}, fileName={}", fileId, fileName);

        try {
            Map<String, Object> data = new HashMap<>();
            data.put("fileId", fileId);
            data.put("fileName", fileName);
            data.put("ownerId", ownerId);

            // Publish event to connected users
            boolean delivered = publishFileEvent(fileId, WebSocketActionType.DELETED, data);

            // If not delivered to all subscribers, queue notifications
            if (!delivered) {
                queueFileNotification(fileId, WebSocketActionType.DELETED, data);
            }

            // Also notify about folder changes if file was in a folder
            if (parentFolderId != null) {
                Map<String, Object> folderData = new HashMap<>();
                folderData.put("folderId", parentFolderId);
                folderData.put("fileId", fileId);
                folderData.put("fileName", fileName);
                folderData.put("ownerId", ownerId);

                // Publish folder event to connected users
                boolean folderDelivered = publishFolderEvent(
                        parentFolderId, WebSocketActionType.UPDATED, folderData);

                // If not delivered to all subscribers, queue notifications
                if (!folderDelivered) {
                    queueFolderNotification(parentFolderId, WebSocketActionType.UPDATED, folderData);
                }
            }

            // Record metrics for event
            recordMetric(0, 1, 0);
        } catch (Exception e) {
            log.error("Error notifying file deletion: {}", e.getMessage(), e);
            recordMetric(0, 0, 1);
        }
    }

    /**
     * Notify about file move
     */
    @Transactional
    public void notifyFileMove(FileResponse fileResponse, Long oldFolderId) {
        log.debug("Notifying file move: fileId={}, fileName={}, oldFolderId={}, newFolderId={}",
                fileResponse.getId(), fileResponse.getFilename(), oldFolderId, fileResponse.getParentFolderId());

        try {
            Map<String, Object> data = new HashMap<>();
            data.put("file", fileResponse);
            data.put("oldFolderId", oldFolderId);
            data.put("newFolderId", fileResponse.getParentFolderId());
            data.put("ownerId", fileResponse.getOwnerId());

            // Publish event to connected users
            boolean delivered = publishFileEvent(fileResponse.getId(), WebSocketActionType.MOVED, data);

            // If not delivered to all subscribers, queue notifications
            if (!delivered) {
                queueFileNotification(fileResponse.getId(), WebSocketActionType.MOVED, data);
            }

            // Notify both old and new folders
            if (oldFolderId != null) {
                Map<String, Object> oldFolderData = new HashMap<>();
                oldFolderData.put("folderId", oldFolderId);
                oldFolderData.put("fileId", fileResponse.getId());
                oldFolderData.put("fileName", fileResponse.getFilename());
                oldFolderData.put("ownerId", fileResponse.getOwnerId());

                // Publish folder event to connected users
                boolean oldFolderDelivered = publishFolderEvent(
                        oldFolderId, WebSocketActionType.UPDATED, oldFolderData);

                // If not delivered to all subscribers, queue notifications
                if (!oldFolderDelivered) {
                    queueFolderNotification(oldFolderId, WebSocketActionType.UPDATED, oldFolderData);
                }
            }

            if (fileResponse.getParentFolderId() != null) {
                Map<String, Object> newFolderData = new HashMap<>();
                newFolderData.put("folderId", fileResponse.getParentFolderId());
                newFolderData.put("fileId", fileResponse.getId());
                newFolderData.put("fileName", fileResponse.getFilename());
                newFolderData.put("ownerId", fileResponse.getOwnerId());

                // Publish folder event to connected users
                boolean newFolderDelivered = publishFolderEvent(
                        fileResponse.getParentFolderId(), WebSocketActionType.UPDATED, newFolderData);

                // If not delivered to all subscribers, queue notifications
                if (!newFolderDelivered) {
                    queueFolderNotification(fileResponse.getParentFolderId(), WebSocketActionType.UPDATED, newFolderData);
                }
            }

            // Record metrics for event
            recordMetric(0, 1, 0);
        } catch (Exception e) {
            log.error("Error notifying file move: {}", e.getMessage(), e);
            recordMetric(0, 0, 1);
        }
    }

    /**
     * Notify about folder creation
     */
    @Transactional
    public void notifyFolderCreation(com.fileflow.dto.response.folder.FolderResponse folderResponse) {
        log.debug("Notifying folder creation: folderId={}, folderName={}",
                folderResponse.getId(), folderResponse.getFolderName());

        try {
            Map<String, Object> data = new HashMap<>();
            data.put("folder", folderResponse);
            data.put("ownerId", folderResponse.getOwnerId());

            // Publish event to connected users
            boolean delivered = publishFolderEvent(folderResponse.getId(), WebSocketActionType.CREATED, data);

            // If not delivered to all subscribers, queue notifications
            if (!delivered) {
                queueFolderNotification(folderResponse.getId(), WebSocketActionType.CREATED, data);
            }

            // Also notify parent folder if applicable
            if (folderResponse.getParentFolderId() != null) {
                Map<String, Object> parentData = new HashMap<>();
                parentData.put("folderId", folderResponse.getParentFolderId());
                parentData.put("childFolderId", folderResponse.getId());
                parentData.put("folderName", folderResponse.getFolderName());
                parentData.put("ownerId", folderResponse.getOwnerId());

                // Publish folder event to connected users
                boolean parentDelivered = publishFolderEvent(
                        folderResponse.getParentFolderId(), WebSocketActionType.UPDATED, parentData);

                // If not delivered to all subscribers, queue notifications
                if (!parentDelivered) {
                    queueFolderNotification(folderResponse.getParentFolderId(), WebSocketActionType.UPDATED, parentData);
                }
            }

            // Record metrics for event
            recordMetric(0, 1, 0);
        } catch (Exception e) {
            log.error("Error notifying folder creation: {}", e.getMessage(), e);
            recordMetric(0, 0, 1);
        }
    }

    /**
     * Notify about folder update
     */
    @Transactional
    public void notifyFolderUpdate(com.fileflow.dto.response.folder.FolderResponse folderResponse) {
        log.debug("Notifying folder update: folderId={}, folderName={}",
                folderResponse.getId(), folderResponse.getFolderName());

        try {
            Map<String, Object> data = new HashMap<>();
            data.put("folder", folderResponse);
            data.put("ownerId", folderResponse.getOwnerId());

            // Publish event to connected users
            boolean delivered = publishFolderEvent(folderResponse.getId(), WebSocketActionType.UPDATED, data);

            // If not delivered to all subscribers, queue notifications
            if (!delivered) {
                queueFolderNotification(folderResponse.getId(), WebSocketActionType.UPDATED, data);
            }

            // Record metrics for event
            recordMetric(0, 1, 0);
        } catch (Exception e) {
            log.error("Error notifying folder update: {}", e.getMessage(), e);
            recordMetric(0, 0, 1);
        }
    }

    /**
     * Notify about folder deletion
     */
    @Transactional
    public void notifyFolderDelete(Long folderId, Long ownerId, String folderName, Long parentFolderId) {
        log.debug("Notifying folder deletion: folderId={}, folderName={}", folderId, folderName);

        try {
            Map<String, Object> data = new HashMap<>();
            data.put("folderId", folderId);
            data.put("folderName", folderName);
            data.put("ownerId", ownerId);

            // Publish event to connected users
            boolean delivered = publishFolderEvent(folderId, WebSocketActionType.DELETED, data);

            // If not delivered to all subscribers, queue notifications
            if (!delivered) {
                queueFolderNotification(folderId, WebSocketActionType.DELETED, data);
            }

            // Also notify parent folder if applicable
            if (parentFolderId != null) {
                Map<String, Object> parentData = new HashMap<>();
                parentData.put("folderId", parentFolderId);
                parentData.put("childFolderId", folderId);
                parentData.put("folderName", folderName);
                parentData.put("ownerId", ownerId);

                // Publish folder event to connected users
                boolean parentDelivered = publishFolderEvent(
                        parentFolderId, WebSocketActionType.UPDATED, parentData);

                // If not delivered to all subscribers, queue notifications
                if (!parentDelivered) {
                    queueFolderNotification(parentFolderId, WebSocketActionType.UPDATED, parentData);
                }
            }

            // Record metrics for event
            recordMetric(0, 1, 0);
        } catch (Exception e) {
            log.error("Error notifying folder deletion: {}", e.getMessage(), e);
            recordMetric(0, 0, 1);
        }
    }

    /**
     * Notify user about quota updates
     */
    @Transactional
    public void notifyQuotaUpdate(User user, long usedSpace, long totalSpace) {
        log.debug("Notifying quota update: userId={}, usedSpace={}, totalSpace={}",
                user.getId(), usedSpace, totalSpace);

        try {
            if (isUserConnected(user.getId())) {
                eventPublisher.sendQuotaUpdateNotification(user.getId(), usedSpace, totalSpace);
            } else {
                // Queue notification for later delivery
                Map<String, Object> data = new HashMap<>();
                data.put("usedSpace", usedSpace);
                data.put("totalSpace", totalSpace);
                data.put("usagePercentage", totalSpace > 0 ? (double) usedSpace * 100 / totalSpace : 0);

                User userRef = new User();
                userRef.setId(user.getId());

                WebSocketNotificationQueue notification = WebSocketNotificationQueue.builder()
                        .user(userRef)
                        .notificationType(WebSocketMessageType.QUOTA_UPDATE.name())
                        .payload(JsonUtil.toJson(data))
                        .build();

                notificationQueueRepository.save(notification);
            }

            // Record metrics for event
            recordMetric(0, 1, 0);
        } catch (Exception e) {
            log.error("Error notifying quota update: {}", e.getMessage(), e);
            recordMetric(0, 0, 1);
        }
    }

    /**
     * Send system notification to a user
     */
    @Transactional
    public void sendSystemNotification(Long userId, String title, String message, String level) {
        log.debug("Sending system notification: userId={}, title={}", userId, title);

        try {
            Map<String, Object> data = new HashMap<>();
            data.put("title", title);
            data.put("message", message);
            data.put("level", level);
            data.put("timestamp", System.currentTimeMillis());

            if (isUserConnected(userId)) {
                // User is connected, send directly
                eventPublisher.sendSystemNotification(userId, title, message, level);
            } else {
                // Queue notification for later delivery
                User userRef = new User();
                userRef.setId(userId);

                WebSocketNotificationQueue notification = WebSocketNotificationQueue.builder()
                        .user(userRef)
                        .notificationType(WebSocketMessageType.SYSTEM_NOTIFICATION.name())
                        .payload(JsonUtil.toJson(data))
                        .build();

                notificationQueueRepository.save(notification);
            }

            // Record metrics for event
            recordMetric(0, 1, 0);
        } catch (Exception e) {
            log.error("Error sending system notification: {}", e.getMessage(), e);
            recordMetric(0, 0, 1);
        }
    }

    /**
     * Subscribe a user to file events
     */
    @Transactional
    public void subscribeToFileEvents(Long userId, Long fileId) {
        log.debug("User {} subscribing to file events for file {}", userId, fileId);

        try {
            // First, update in-memory subscription in event publisher
            eventPublisher.subscribeToFileEvents(userId, fileId);

            // Then, persist the subscription
            Optional<WebSocketSubscription> existingSubscription = subscriptionRepository
                    .findByUser_IdAndItemIdAndItemType(userId, fileId, WebSocketSubscription.ItemType.FILE);

            if (existingSubscription.isPresent()) {
                WebSocketSubscription subscription = existingSubscription.get();

                // Reactivate if inactive
                if (!subscription.isActive()) {
                    subscription.setActive(true);
                    subscription.setUpdatedAt(LocalDateTime.now());
                    subscriptionRepository.save(subscription);
                }
            } else {
                // Create new subscription
                User userRef = new User();
                userRef.setId(userId);

                WebSocketSubscription subscription = WebSocketSubscription.builder()
                        .user(userRef)
                        .itemId(fileId)
                        .itemType(WebSocketSubscription.ItemType.FILE)
                        .isActive(true)
                        .build();

                subscriptionRepository.save(subscription);
            }

            // Record metric for subscription action
            recordMetric(1, 0, 0);
        } catch (Exception e) {
            log.error("Error subscribing to file events: {}", e.getMessage(), e);
            recordMetric(0, 0, 1);
        }
    }

    /**
     * Subscribe a user to folder events
     */
    @Transactional
    public void subscribeToFolderEvents(Long userId, Long folderId) {
        log.debug("User {} subscribing to folder events for folder {}", userId, folderId);

        try {
            // First, update in-memory subscription in event publisher
            eventPublisher.subscribeToFolderEvents(userId, folderId);

            // Then, persist the subscription
            Optional<WebSocketSubscription> existingSubscription = subscriptionRepository
                    .findByUser_IdAndItemIdAndItemType(userId, folderId, WebSocketSubscription.ItemType.FOLDER);

            if (existingSubscription.isPresent()) {
                WebSocketSubscription subscription = existingSubscription.get();

                // Reactivate if inactive
                if (!subscription.isActive()) {
                    subscription.setActive(true);
                    subscription.setUpdatedAt(LocalDateTime.now());
                    subscriptionRepository.save(subscription);
                }
            } else {
                // Create new subscription
                User userRef = new User();
                userRef.setId(userId);

                WebSocketSubscription subscription = WebSocketSubscription.builder()
                        .user(userRef)
                        .itemId(folderId)
                        .itemType(WebSocketSubscription.ItemType.FOLDER)
                        .isActive(true)
                        .build();

                subscriptionRepository.save(subscription);
            }

            // Record metric for subscription action
            recordMetric(1, 0, 0);
        } catch (Exception e) {
            log.error("Error subscribing to folder events: {}", e.getMessage(), e);
            recordMetric(0, 0, 1);
        }
    }

    /**
     * Unsubscribe a user from file events
     */
    @Transactional
    public void unsubscribeFromFileEvents(Long userId, Long fileId) {
        log.debug("User {} unsubscribing from file events for file {}", userId, fileId);

        try {
            // First, update in-memory subscription in event publisher
            eventPublisher.unsubscribeFromFileEvents(userId, fileId);

            // Then, update database
            subscriptionRepository.deactivateSubscription(
                    userId, fileId, WebSocketSubscription.ItemType.FILE, LocalDateTime.now());

            // Record metric for unsubscription action
            recordMetric(1, 0, 0);
        } catch (Exception e) {
            log.error("Error unsubscribing from file events: {}", e.getMessage(), e);
            recordMetric(0, 0, 1);
        }
    }

    /**
     * Unsubscribe a user from folder events
     */
    @Transactional
    public void unsubscribeFromFolderEvents(Long userId, Long folderId) {
        log.debug("User {} unsubscribing from folder events for folder {}", userId, folderId);

        try {
            // First, update in-memory subscription in event publisher
            eventPublisher.unsubscribeFromFolderEvents(userId, folderId);

            // Then, update database
            subscriptionRepository.deactivateSubscription(
                    userId, folderId, WebSocketSubscription.ItemType.FOLDER, LocalDateTime.now());

            // Record metric for unsubscription action
            recordMetric(1, 0, 0);
        } catch (Exception e) {
            log.error("Error unsubscribing from folder events: {}", e.getMessage(), e);
            recordMetric(0, 0, 1);
        }
    }

    /**
     * Remove all subscriptions for a user
     */
    @Transactional
    public void removeUserSubscriptions(Long userId) {
        log.debug("Removing all subscriptions for user {}", userId);

        try {
            // First, update in-memory subscriptions in event publisher
            eventPublisher.removeUserSubscriptions(userId);

            // Then, update database
            subscriptionRepository.deactivateAllForUser(userId, LocalDateTime.now());

            // Clear connection cache
            userConnectionCache.remove(userId);

            // Record metric for mass unsubscription action
            recordMetric(1, 0, 0);
        } catch (Exception e) {
            log.error("Error removing user subscriptions: {}", e.getMessage(), e);
            recordMetric(0, 0, 1);
        }
    }

    /**
     * Process queued notifications for a user
     */
    @Transactional
    public int processQueuedNotifications(Long userId) {
        if (!isUserConnected(userId)) {
            return 0; // User not connected, cannot process
        }

        int processed = 0;

        try {
            List<WebSocketNotificationQueue> notifications = notificationQueueRepository
                    .findByUser_IdAndIsSentFalseOrderByCreatedAt(userId, PageRequest.of(0, BATCH_SIZE));

            for (WebSocketNotificationQueue notification : notifications) {
                try {
                    // Parse the payload
                    Map<String, Object> payload = JsonUtil.fromJson(
                            notification.getPayload(),
                            new com.google.gson.reflect.TypeToken<Map<String, Object>>(){}.getType()
                    );
                    WebSocketMessageType type = WebSocketMessageType.valueOf(notification.getNotificationType());

                    // Send based on type
                    switch (type) {
                        case FILE_EVENT:
                            sendFileEvent(userId, payload);
                            break;

                        case FOLDER_EVENT:
                            sendFolderEvent(userId, payload);
                            break;

                        case SYSTEM_NOTIFICATION:
                            sendSystemNotificationFromPayload(userId, payload);
                            break;

                        case QUOTA_UPDATE:
                            sendQuotaUpdateFromPayload(userId, payload);
                            break;

                        default:
                            log.warn("Unsupported notification type: {}", type);
                            continue;
                    }

                    // Mark as sent
                    notificationQueueRepository.markAsSent(notification.getId(), LocalDateTime.now());
                    processed++;

                    // Record metric for sent message
                    recordMetric(0, 1, 0);

                } catch (Exception e) {
                    log.error("Error processing notification {}: {}", notification.getId(), e.getMessage(), e);

                    // Update retry count
                    notificationQueueRepository.incrementRetryCount(notification.getId(), LocalDateTime.now());

                    // Record error
                    recordMetric(0, 0, 1);
                }
            }
        } catch (Exception e) {
            log.error("Error retrieving queued notifications for user {}: {}", userId, e.getMessage(), e);
            recordMetric(0, 0, 1);
        }

        return processed;
    }

    /**
     * Process notifications that need to be retried
     */
    @Transactional
    @Async
    public void processRetryNotifications() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(5); // Only retry notifications older than 5 minutes

        try {
            List<WebSocketNotificationQueue> notificationsToRetry = notificationQueueRepository
                    .findNotificationsToRetry(MAX_RETRY_ATTEMPTS, cutoffTime, PageRequest.of(0, BATCH_SIZE));

            int processed = 0;

            for (WebSocketNotificationQueue notification : notificationsToRetry) {
                if (!isUserConnected(notification.getUser().getId())) {
                    continue; // Skip if user not connected
                }

                try {
                    // Parse the payload
                    @SuppressWarnings("unchecked")
                    Map<String, Object> payload = JsonUtil.fromJson(notification.getPayload(), Map.class);
                    WebSocketMessageType type = WebSocketMessageType.valueOf(notification.getNotificationType());

                    // Send based on type
                    switch (type) {
                        case FILE_EVENT:
                            sendFileEvent(notification.getUser().getId(), payload);
                            break;

                        case FOLDER_EVENT:
                            sendFolderEvent(notification.getUser().getId(), payload);
                            break;

                        case SYSTEM_NOTIFICATION:
                            sendSystemNotificationFromPayload(notification.getUser().getId(), payload);
                            break;

                        case QUOTA_UPDATE:
                            sendQuotaUpdateFromPayload(notification.getUser().getId(), payload);
                            break;

                        default:
                            log.warn("Unsupported notification type: {}", type);
                            continue;
                    }

                    // Mark as sent
                    notificationQueueRepository.markAsSent(notification.getId(), LocalDateTime.now());
                    processed++;

                    // Record metric for sent message
                    recordMetric(0, 1, 0);

                } catch (Exception e) {
                    log.error("Error retrying notification {}: {}", notification.getId(), e.getMessage(), e);

                    // Update retry count
                    notificationQueueRepository.incrementRetryCount(notification.getId(), LocalDateTime.now());

                    // Record error
                    recordMetric(0, 0, 1);
                }
            }

            if (processed > 0) {
                log.debug("Processed {} retry notifications", processed);
            }
        } catch (Exception e) {
            log.error("Error processing retry notifications: {}", e.getMessage(), e);
            recordMetric(0, 0, 1);
        }
    }

    /**
     * Check if a user is connected
     */
    public boolean isUserConnected(Long userId) {
        // First check the cache
        if (userConnectionCache.containsKey(userId)) {
            return userConnectionCache.get(userId);
        }

        // Then check with event publisher
        boolean connected = eventPublisher.isUserConnected(userId);

        // Update cache
        userConnectionCache.put(userId, connected);

        return connected;
    }

    /**
     * Record a new connection
     */
    @Transactional
    public void recordConnection(String sessionId, Long userId, String ipAddress, String userAgent) {
        try {
            // Update cache
            userConnectionCache.put(userId, true);

            // Create user reference
            User userRef = new User();
            userRef.setId(userId);

            // Store session information
            com.fileflow.model.WebSocketSession session = com.fileflow.model.WebSocketSession.builder()
                    .sessionId(sessionId)
                    .user(userRef)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .isActive(true)
                    .lastActivity(LocalDateTime.now())
                    .build();

            sessionRepository.save(session);

            // Update metrics
            recordMetric(0, 0, 0);
        } catch (Exception e) {
            log.error("Error recording WebSocket connection: {}", e.getMessage(), e);
        }
    }

    /**
     * Record session disconnect
     */
    @Transactional
    public void recordDisconnect(String sessionId) {
        try {
            Optional<com.fileflow.model.WebSocketSession> session = sessionRepository.findBySessionId(sessionId);

            if (session.isPresent()) {
                Long userId = session.get().getUser().getId();

                // Update cache
                userConnectionCache.remove(userId);

                // Mark session as disconnected
                sessionRepository.markAsDisconnected(sessionId, LocalDateTime.now());

                // Update metrics
                recordMetric(0, 0, 0);
            }
        } catch (Exception e) {
            log.error("Error recording WebSocket disconnection: {}", e.getMessage(), e);
        }
    }

    /**
     * Update session activity
     */
    @Transactional
    public void updateSessionActivity(String sessionId) {
        try {
            sessionRepository.updateLastActivity(sessionId, LocalDateTime.now());
        } catch (Exception e) {
            log.error("Error updating WebSocket session activity: {}", e.getMessage(), e);
        }
    }

    /**
     * Record WebSocket metrics
     */
    @Transactional
    public void recordMetric(int messagesReceived, int messagesSent, int errors) {
        try {
            LocalDate today = LocalDate.now();
            int hour = LocalTime.now().getHour();

            Optional<WebSocketMetrics> existingMetrics = metricsRepository.findByEventDateAndHourOfDay(today, hour);

            WebSocketMetrics metrics;
            if (existingMetrics.isPresent()) {
                metrics = existingMetrics.get();

                // Update metrics
                if (messagesReceived > 0) {
                    metrics.setMessagesReceived(metrics.getMessagesReceived() + messagesReceived);
                }

                if (messagesSent > 0) {
                    metrics.setMessagesSent(metrics.getMessagesSent() + messagesSent);
                }

                if (errors > 0) {
                    metrics.setErrorsCount(metrics.getErrorsCount() + errors);
                }
            } else {
                // Create new metrics
                metrics = WebSocketMetrics.builder()
                        .eventDate(today)
                        .hourOfDay(hour)
                        .activeConnections(countActiveConnections())
                        .messagesReceived(messagesReceived)
                        .messagesSent(messagesSent)
                        .errorsCount(errors)
                        .build();
            }

            metricsRepository.save(metrics);
        } catch (Exception e) {
            log.error("Error recording WebSocket metrics: {}", e.getMessage());
        }
    }

    /**
     * Get metrics for a date range
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getMetricsForDateRange(LocalDate startDate, LocalDate endDate) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> metrics = (Map<String, Object>) metricsRepository.getTotalMetricsForDateRange(startDate, endDate);
            return metrics;
        } catch (Exception e) {
            log.error("Error getting WebSocket metrics: {}", e.getMessage(), e);
            return Map.of("error", "Failed to retrieve metrics");
        }
    }

    /**
     * Get WebSocket stats for the current user
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getWebSocketStats() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
                return Map.of("error", "User not authenticated");
            }

            Long userId = userPrincipal.getId();

            // Get active sessions
            List<com.fileflow.model.WebSocketSession> activeSessions =
                    sessionRepository.findByUser_IdAndIsActiveTrue(userId);

            // Get active subscriptions
            List<WebSocketSubscription> activeSubscriptions =
                    subscriptionRepository.findByUser_IdAndIsActiveTrue(userId);

            // Count by type
            long fileSubscriptions = activeSubscriptions.stream()
                    .filter(s -> s.getItemType() == WebSocketSubscription.ItemType.FILE)
                    .count();

            long folderSubscriptions = activeSubscriptions.stream()
                    .filter(s -> s.getItemType() == WebSocketSubscription.ItemType.FOLDER)
                    .count();

            // Get pending notifications
            long pendingNotifications = notificationQueueRepository.countByUser_IdAndIsSentFalse(userId);

            // Build response
            Map<String, Object> stats = new HashMap<>();
            stats.put("userId", userId);
            stats.put("connected", isUserConnected(userId));
            stats.put("activeSessions", activeSessions.size());
            stats.put("activeSubscriptions", activeSubscriptions.size());
            stats.put("fileSubscriptions", fileSubscriptions);
            stats.put("folderSubscriptions", folderSubscriptions);
            stats.put("pendingNotifications", pendingNotifications);

            return stats;
        } catch (Exception e) {
            log.error("Error getting WebSocket stats: {}", e.getMessage(), e);
            return Map.of("error", "Failed to retrieve WebSocket statistics");
        }
    }

    /**
     * Count active connections
     */
    private int countActiveConnections() {
        try {
            return (int) sessionRepository.count();
        } catch (Exception e) {
            log.error("Error counting active connections: {}", e.getMessage(), e);
            return 0;
        }
    }

    // Helper methods for publishing events and queueing notifications

    /**
     * Publish file event to connected subscribers
     * @return true if successfully delivered to all subscribers
     */
    private boolean publishFileEvent(Long fileId, WebSocketActionType action, Map<String, Object> data) {
        try {
            eventPublisher.publishFileEvent(fileId, action, data);
            return true;
        } catch (Exception e) {
            log.error("Error publishing file event: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Publish folder event to connected subscribers
     * @return true if successfully delivered to all subscribers
     */
    private boolean publishFolderEvent(Long folderId, WebSocketActionType action, Map<String, Object> data) {
        try {
            eventPublisher.publishFolderEvent(folderId, action, data);
            return true;
        } catch (Exception e) {
            log.error("Error publishing folder event: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Queue file notification for offline subscribers
     */
    private void queueFileNotification(Long fileId, WebSocketActionType action, Map<String, Object> data) {
        try {
            // Find all subscribers to this file
            List<WebSocketSubscription> subscriptions = subscriptionRepository
                    .findByItemIdAndItemTypeAndIsActiveTrue(fileId, WebSocketSubscription.ItemType.FILE);

            // Create notification payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", WebSocketMessageType.FILE_EVENT);
            payload.put("fileId", fileId);
            payload.put("action", action.name());
            payload.put("data", data);

            String jsonPayload = JsonUtil.toJson(payload);

            // Queue for each offline subscriber
            for (WebSocketSubscription subscription : subscriptions) {
                if (!isUserConnected(subscription.getUser().getId())) {
                    WebSocketNotificationQueue notification = WebSocketNotificationQueue.builder()
                            .user(subscription.getUser())
                            .notificationType(WebSocketMessageType.FILE_EVENT.name())
                            .payload(jsonPayload)
                            .build();

                    notificationQueueRepository.save(notification);
                }
            }

            // Always queue for the owner if they're specified in the data
            if (data.containsKey("ownerId")) {
                Long ownerId = Long.valueOf(data.get("ownerId").toString());

                if (!isUserConnected(ownerId)) {
                    User ownerRef = new User();
                    ownerRef.setId(ownerId);

                    WebSocketNotificationQueue notification = WebSocketNotificationQueue.builder()
                            .user(ownerRef)
                            .notificationType(WebSocketMessageType.FILE_EVENT.name())
                            .payload(jsonPayload)
                            .build();

                    notificationQueueRepository.save(notification);
                }
            }
        } catch (Exception e) {
            log.error("Error queueing file notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Queue folder notification for offline subscribers
     */
    private void queueFolderNotification(Long folderId, WebSocketActionType action, Map<String, Object> data) {
        try {
            // Find all subscribers to this folder
            List<WebSocketSubscription> subscriptions = subscriptionRepository
                    .findByItemIdAndItemTypeAndIsActiveTrue(folderId, WebSocketSubscription.ItemType.FOLDER);

            // Create notification payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", WebSocketMessageType.FOLDER_EVENT);
            payload.put("folderId", folderId);
            payload.put("action", action.name());
            payload.put("data", data);

            String jsonPayload = JsonUtil.toJson(payload);

            // Queue for each offline subscriber
            for (WebSocketSubscription subscription : subscriptions) {
                if (!isUserConnected(subscription.getUser().getId())) {
                    WebSocketNotificationQueue notification = WebSocketNotificationQueue.builder()
                            .user(subscription.getUser())
                            .notificationType(WebSocketMessageType.FOLDER_EVENT.name())
                            .payload(jsonPayload)
                            .build();

                    notificationQueueRepository.save(notification);
                }
            }

            // Always queue for the owner if they're specified in the data
            if (data.containsKey("ownerId")) {
                Long ownerId = Long.valueOf(data.get("ownerId").toString());

                if (!isUserConnected(ownerId)) {
                    User ownerRef = new User();
                    ownerRef.setId(ownerId);

                    WebSocketNotificationQueue notification = WebSocketNotificationQueue.builder()
                            .user(ownerRef)
                            .notificationType(WebSocketMessageType.FOLDER_EVENT.name())
                            .payload(jsonPayload)
                            .build();

                    notificationQueueRepository.save(notification);
                }
            }
        } catch (Exception e) {
            log.error("Error queueing folder notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Send file event from a payload
     */
    private void sendFileEvent(Long userId, Map<String, Object> payload) {
        try {
            Long fileId = Long.valueOf(payload.get("fileId").toString());
            WebSocketActionType action = WebSocketActionType.valueOf(payload.get("action").toString());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) payload.get("data");

            eventPublisher.publishFileEvent(fileId, action, data);
        } catch (Exception e) {
            log.error("Error sending file event: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Send folder event from a payload
     */
    private void sendFolderEvent(Long userId, Map<String, Object> payload) {
        try {
            Long folderId = Long.valueOf(payload.get("folderId").toString());
            WebSocketActionType action = WebSocketActionType.valueOf(payload.get("action").toString());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) payload.get("data");

            eventPublisher.publishFolderEvent(folderId, action, data);
        } catch (Exception e) {
            log.error("Error sending folder event: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Send system notification from a payload
     */
    private void sendSystemNotificationFromPayload(Long userId, Map<String, Object> payload) {
        try {
            String title = payload.get("title").toString();
            String message = payload.get("message").toString();
            String level = payload.get("level").toString();

            eventPublisher.sendSystemNotification(userId, title, message, level);
        } catch (Exception e) {
            log.error("Error sending system notification: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Send quota update from a payload
     */
    private void sendQuotaUpdateFromPayload(Long userId, Map<String, Object> payload) {
        try {
            long usedSpace = Long.parseLong(payload.get("usedSpace").toString());
            long totalSpace = Long.parseLong(payload.get("totalSpace").toString());

            eventPublisher.sendQuotaUpdateNotification(userId, usedSpace, totalSpace);
        } catch (Exception e) {
            log.error("Error sending quota update: {}", e.getMessage(), e);
            throw e;
        }
    }
}