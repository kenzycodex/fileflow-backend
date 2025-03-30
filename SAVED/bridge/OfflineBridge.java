package com.fileflow.bridge;

import com.fileflow.bridge.core.BridgeInterface;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.model.SyncQueue;
import com.fileflow.model.User;
import com.fileflow.repository.SyncQueueRepository;
import com.fileflow.repository.UserRepository;
import com.fileflow.security.UserPrincipal;
import com.fileflow.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bridge between Java backend and WebView for offline operations
 * Used by desktop and mobile applications
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OfflineBridge implements BridgeInterface {

    private final SyncQueueRepository syncQueueRepository;
    private final UserRepository userRepository;

    @Override
    public String getBridgeName() {
        return "offline";
    }

    /**
     * Queue sync operation for later processing
     *
     * @param actionType action type (CREATE, UPDATE, DELETE)
     * @param itemType item type (FILE, FOLDER)
     * @param itemId item ID
     * @param dataJson additional data for sync as JSON string
     * @return queuing status as JSON
     */
    public String queueSyncOperation(String actionType, String itemType, String itemId, String dataJson) {
        try {
            User currentUser = getCurrentUser();
            Long itemIdLong = itemId != null && !itemId.isEmpty() ? Long.parseLong(itemId) : null;

            // Parse data JSON if provided
            Map<String, Object> data = null;
            if (dataJson != null && !dataJson.isEmpty()) {
                try {
                    data = JsonUtil.fromJson(dataJson, Map.class);
                } catch (Exception e) {
                    log.warn("Failed to parse data JSON: {}", dataJson, e);
                }
            }

            // Create sync queue entry
            SyncQueue syncQueue = SyncQueue.builder()
                    .userId(currentUser.getId())
                    .actionType(actionType)
                    .itemType(itemType)
                    .itemId(itemIdLong)
                    .status(SyncQueue.Status.PENDING)
                    .created_at(LocalDateTime.now())
                    .dataPayload(data != null ? data.toString() : null)
                    .build();

            SyncQueue savedSyncQueue = syncQueueRepository.save(syncQueue);

            log.info("Queued sync operation: {} - {} - {}", actionType, itemType, itemId);

            Map<String, Object> resultData = new HashMap<>();
            resultData.put("queueId", savedSyncQueue.getId());
            resultData.put("actionType", actionType);
            resultData.put("itemType", itemType);
            resultData.put("itemId", itemId);

            ApiResponse response = ApiResponse.builder()
                    .success(true)
                    .message("Sync operation queued successfully")
                    .data(resultData)
                    .timestamp(LocalDateTime.now())
                    .build();

            return JsonUtil.toJson(response);
        } catch (Exception e) {
            log.error("Error queuing sync operation", e);

            ApiResponse response = ApiResponse.builder()
                    .success(false)
                    .message("Error queuing sync operation: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();

            return JsonUtil.toJson(response);
        }
    }

    /**
     * Get pending sync operations as JSON
     */
    public String getPendingSyncOperations() {
        try {
            User currentUser = getCurrentUser();

            List<SyncQueue> pendingOperations = syncQueueRepository.findByUserIdAndStatus(
                    currentUser.getId(), SyncQueue.Status.PENDING);

            Map<String, Object> result = new HashMap<>();
            result.put("operations", pendingOperations);
            result.put("count", pendingOperations.size());
            result.put("timestamp", LocalDateTime.now().toString());

            return JsonUtil.toJson(result);
        } catch (Exception e) {
            log.error("Error getting pending sync operations", e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error getting pending sync operations: " + e.getMessage());
            error.put("timestamp", LocalDateTime.now().toString());

            return JsonUtil.toJson(error);
        }
    }

    /**
     * Process sync queue
     * @return processing status as JSON
     */
    public String processSyncQueue() {
        try {
            User currentUser = getCurrentUser();

            List<SyncQueue> pendingOperations = syncQueueRepository.findByUserIdAndStatus(
                    currentUser.getId(), SyncQueue.Status.PENDING);

            int processed = 0;
            int failed = 0;

            for (SyncQueue operation : pendingOperations) {
                try {
                    // Process operation based on type
                    // Implementation depends on specific action types

                    // Mark as processed
                    operation.setStatus(SyncQueue.Status.COMPLETED);
                    operation.setProcessedAt(LocalDateTime.now());
                    syncQueueRepository.save(operation);

                    processed++;
                } catch (Exception e) {
                    log.error("Error processing sync operation: {}", operation.getId(), e);

                    // Mark as failed
                    operation.setStatus(SyncQueue.Status.FAILED);
                    operation.setRetryCount(operation.getRetryCount() + 1);
                    syncQueueRepository.save(operation);

                    failed++;
                }
            }

            log.info("Processed sync queue: {} processed, {} failed", processed, failed);

            Map<String, Object> resultData = new HashMap<>();
            resultData.put("processed", processed);
            resultData.put("failed", failed);
            resultData.put("total", pendingOperations.size());

            ApiResponse response = ApiResponse.builder()
                    .success(true)
                    .message("Sync queue processed")
                    .data(resultData)
                    .timestamp(LocalDateTime.now())
                    .build();

            return JsonUtil.toJson(response);
        } catch (Exception e) {
            log.error("Error processing sync queue", e);

            ApiResponse response = ApiResponse.builder()
                    .success(false)
                    .message("Error processing sync queue: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();

            return JsonUtil.toJson(response);
        }
    }

    /**
     * Set offline mode
     *
     * @param offlineMode true to enable offline mode, false to disable
     * @return offline mode status as JSON
     */
    public String setOfflineMode(String offlineMode) {
        try {
            boolean isOffline = Boolean.parseBoolean(offlineMode);

            // This would be handled in the native application
            // to enable/disable network requests

            log.info("Set offline mode: {}", isOffline);

            Map<String, Object> resultData = new HashMap<>();
            resultData.put("offlineMode", isOffline);
            resultData.put("timestamp", LocalDateTime.now().toString());

            ApiResponse response = ApiResponse.builder()
                    .success(true)
                    .message("Offline mode set to: " + isOffline)
                    .data(resultData)
                    .timestamp(LocalDateTime.now())
                    .build();

            return JsonUtil.toJson(response);
        } catch (Exception e) {
            log.error("Error setting offline mode", e);

            ApiResponse response = ApiResponse.builder()
                    .success(false)
                    .message("Error setting offline mode: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();

            return JsonUtil.toJson(response);
        }
    }

    /**
     * Check connection status
     * @return connection status as JSON
     */
    public String checkConnectionStatus() {
        try {
            // This would be implemented in the native application
            // to check actual connection status

            Map<String, Object> resultData = new HashMap<>();
            resultData.put("online", true);
            resultData.put("networkType", "unknown");
            resultData.put("timestamp", LocalDateTime.now().toString());

            return JsonUtil.toJson(resultData);
        } catch (Exception e) {
            log.error("Error checking connection status", e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error checking connection status: " + e.getMessage());
            error.put("timestamp", LocalDateTime.now().toString());

            return JsonUtil.toJson(error);
        }
    }

    /**
     * Check if this bridge is available in the current environment
     * @return true if available
     */
    public boolean isAvailable() {
        return true;
    }

    // Helper methods
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}