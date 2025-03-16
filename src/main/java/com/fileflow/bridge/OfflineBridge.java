package com.fileflow.bridge;

import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.model.SyncQueue;
import com.fileflow.model.User;
import com.fileflow.repository.SyncQueueRepository;
import com.fileflow.repository.UserRepository;
import com.fileflow.security.UserPrincipal;
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
public class OfflineBridge {

    private final SyncQueueRepository syncQueueRepository;
    private final UserRepository userRepository;

    /**
     * Queue sync operation for later processing
     *
     * @param actionType action type (CREATE, UPDATE, DELETE)
     * @param itemType item type (FILE, FOLDER)
     * @param itemId item ID
     * @param data additional data for sync
     * @return queuing status
     */
    public ApiResponse queueSyncOperation(String actionType, String itemType, Long itemId, Map<String, Object> data) {
        try {
            User currentUser = getCurrentUser();

            // Create sync queue entry
            SyncQueue syncQueue = SyncQueue.builder()
                    .user(currentUser)
                    .actionType(actionType)
                    .itemType(itemType)
                    .itemId(itemId)
                    .status("PENDING")
                    .createdAt(LocalDateTime.now())
                    .dataPayload(data != null ? data.toString() : null)
                    .build();

            SyncQueue savedSyncQueue = syncQueueRepository.save(syncQueue);

            log.info("Queued sync operation: {} - {} - {}", actionType, itemType, itemId);

            return ApiResponse.builder()
                    .success(true)
                    .message("Sync operation queued successfully")
                    .data(Map.of("queueId", savedSyncQueue.getId()))
                    .timestamp(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("Error queuing sync operation", e);

            return ApiResponse.builder()
                    .success(false)
                    .message("Error queuing sync operation: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * Get pending sync operations
     *
     * @return list of pending sync operations
     */
    public List<SyncQueue> getPendingSyncOperations() {
        User currentUser = getCurrentUser();

        return syncQueueRepository.findByUserAndStatus(currentUser, "PENDING");
    }

    /**
     * Process sync queue
     *
     * @return processing status
     */
    public ApiResponse processSyncQueue() {
        try {
            User currentUser = getCurrentUser();

            List<SyncQueue> pendingOperations = syncQueueRepository.findByUserAndStatus(currentUser, "PENDING");

            int processed = 0;
            int failed = 0;

            for (SyncQueue operation : pendingOperations) {
                try {
                    // Process operation based on type
                    // Implementation depends on specific action types

                    // Mark as processed
                    operation.setStatus("COMPLETED");
                    operation.setProcessedAt(LocalDateTime.now());
                    syncQueueRepository.save(operation);

                    processed++;
                } catch (Exception e) {
                    log.error("Error processing sync operation: {}", operation.getId(), e);

                    // Mark as failed
                    operation.setStatus("FAILED");
                    operation.setRetryCount(operation.getRetryCount() + 1);
                    syncQueueRepository.save(operation);

                    failed++;
                }
            }

            log.info("Processed sync queue: {} processed, {} failed", processed, failed);

            Map<String, Object> data = new HashMap<>();
            data.put("processed", processed);
            data.put("failed", failed);
            data.put("total", pendingOperations.size());

            return ApiResponse.builder()
                    .success(true)
                    .message("Sync queue processed")
                    .data(data)
                    .timestamp(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("Error processing sync queue", e);

            return ApiResponse.builder()
                    .success(false)
                    .message("Error processing sync queue: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * Set offline mode
     *
     * @param offline true to enable offline mode, false to disable
     * @return offline mode status
     */
    public ApiResponse setOfflineMode(boolean offline) {
        try {
            // This would be handled in the native application
            // to enable/disable network requests

            log.info("Set offline mode: {}", offline);

            return ApiResponse.builder()
                    .success(true)
                    .message("Offline mode set to: " + offline)
                    .data(Map.of("offlineMode", offline))
                    .timestamp(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("Error setting offline mode", e);

            return ApiResponse.builder()
                    .success(false)
                    .message("Error setting offline mode: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    // Helper methods

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}