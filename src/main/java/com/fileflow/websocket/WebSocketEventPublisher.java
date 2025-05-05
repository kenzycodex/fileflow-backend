package com.fileflow.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for publishing events to WebSocket clients
 */
@Component
@Slf4j
public class WebSocketEventPublisher {

    private FileFlowWebSocketHandler webSocketHandler;

    // Use @Lazy annotation to break the circular dependency
    @Autowired
    public void setWebSocketHandler(@Lazy FileFlowWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    // Maps to track file subscriptions (fileId -> set of userIds)
    private final Map<Long, Set<Long>> fileSubscriptions = new ConcurrentHashMap<>();

    // Maps to track folder subscriptions (folderId -> set of userIds)
    private final Map<Long, Set<Long>> folderSubscriptions = new ConcurrentHashMap<>();

    /**
     * Subscribe a user to file events
     */
    public void subscribeToFileEvents(Long userId, Long fileId) {
        fileSubscriptions.computeIfAbsent(fileId, k -> ConcurrentHashMap.newKeySet()).add(userId);
    }

    /**
     * Subscribe a user to folder events
     */
    public void subscribeToFolderEvents(Long userId, Long folderId) {
        folderSubscriptions.computeIfAbsent(folderId, k -> ConcurrentHashMap.newKeySet()).add(userId);
    }

    /**
     * Unsubscribe a user from file events
     */
    public void unsubscribeFromFileEvents(Long userId, Long fileId) {
        Set<Long> subscribers = fileSubscriptions.get(fileId);
        if (subscribers != null) {
            subscribers.remove(userId);
            if (subscribers.isEmpty()) {
                fileSubscriptions.remove(fileId);
            }
        }
    }

    /**
     * Unsubscribe a user from folder events
     */
    public void unsubscribeFromFolderEvents(Long userId, Long folderId) {
        Set<Long> subscribers = folderSubscriptions.get(folderId);
        if (subscribers != null) {
            subscribers.remove(userId);
            if (subscribers.isEmpty()) {
                folderSubscriptions.remove(folderId);
            }
        }
    }

    /**
     * Remove all subscriptions for a user
     */
    public void removeUserSubscriptions(Long userId) {
        // Remove file subscriptions
        for (Map.Entry<Long, Set<Long>> entry : fileSubscriptions.entrySet()) {
            entry.getValue().remove(userId);
            if (entry.getValue().isEmpty()) {
                fileSubscriptions.remove(entry.getKey());
            }
        }

        // Remove folder subscriptions
        for (Map.Entry<Long, Set<Long>> entry : folderSubscriptions.entrySet()) {
            entry.getValue().remove(userId);
            if (entry.getValue().isEmpty()) {
                folderSubscriptions.remove(entry.getKey());
            }
        }
    }

    /**
     * Publish a file event to all subscribed users
     */
    public void publishFileEvent(Long fileId, WebSocketActionType action, Map<String, Object> data) {
        // Always notify the file owner
        Long ownerId = data.containsKey("ownerId") ? Long.valueOf(data.get("ownerId").toString()) : null;
        if (ownerId != null) {
            webSocketHandler.sendFileChangeNotification(fileId, ownerId, action, data);
        }

        // Notify all subscribed users
        Set<Long> subscribers = fileSubscriptions.get(fileId);
        if (subscribers != null) {
            for (Long userId : subscribers) {
                // Skip owner as they've already been notified
                if (ownerId != null && userId.equals(ownerId)) {
                    continue;
                }
                webSocketHandler.sendFileChangeNotification(fileId, userId, action, data);
            }
        }
    }

    /**
     * Publish a folder event to all subscribed users
     */
    public void publishFolderEvent(Long folderId, WebSocketActionType action, Map<String, Object> data) {
        // Always notify the folder owner
        Long ownerId = data.containsKey("ownerId") ? Long.valueOf(data.get("ownerId").toString()) : null;
        if (ownerId != null) {
            webSocketHandler.sendFolderChangeNotification(folderId, ownerId, action, data);
        }

        // Notify all subscribed users
        Set<Long> subscribers = folderSubscriptions.get(folderId);
        if (subscribers != null) {
            for (Long userId : subscribers) {
                // Skip owner as they've already been notified
                if (ownerId != null && userId.equals(ownerId)) {
                    continue;
                }
                webSocketHandler.sendFolderChangeNotification(folderId, userId, action, data);
            }
        }
    }

    /**
     * Send a system notification to a specific user
     */
    public void sendSystemNotification(Long userId, String title, String message, String level) {
        Map<String, Object> data = Map.of(
                "title", title,
                "message", message,
                "level", level,
                "timestamp", System.currentTimeMillis()
        );

        WebSocketMessage wsMessage = new WebSocketMessage(WebSocketMessageType.SYSTEM_NOTIFICATION, data);
        webSocketHandler.sendMessageToUser(userId, wsMessage);
    }

    /**
     * Send a quota update notification to a specific user
     */
    public void sendQuotaUpdateNotification(Long userId, long usedSpace, long totalSpace) {
        Map<String, Object> data = Map.of(
                "usedSpace", usedSpace,
                "totalSpace", totalSpace,
                "usagePercentage", (double) usedSpace / totalSpace * 100
        );

        WebSocketMessage wsMessage = new WebSocketMessage(WebSocketMessageType.QUOTA_UPDATE, data);
        webSocketHandler.sendMessageToUser(userId, wsMessage);
    }

    // Check if user is connected
    public boolean isUserConnected(Long userId) {
        return webSocketHandler.isUserConnected(userId);
    }
}