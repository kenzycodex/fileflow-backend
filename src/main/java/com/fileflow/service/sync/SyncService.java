package com.fileflow.service.sync;

import com.fileflow.dto.request.sync.SyncBatchRequest;
import com.fileflow.dto.request.sync.SyncItemRequest;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.sync.SyncStatusResponse;
import com.fileflow.model.Device;
import com.fileflow.model.SyncQueue;

import java.util.List;

public interface SyncService {
    /**
     * Register device for sync
     *
     * @param deviceName device name
     * @param deviceType device type
     * @param platform platform
     * @return device information
     */
    Device registerDevice(String deviceName, String deviceType, String platform);

    /**
     * Update device push token
     *
     * @param deviceId device ID
     * @param pushToken push token
     * @return updated device
     */
    Device updateDevicePushToken(Long deviceId, String pushToken);

    /**
     * Get sync status for device
     *
     * @param deviceId device ID
     * @return sync status
     */
    SyncStatusResponse getSyncStatus(Long deviceId);

    /**
     * Add item to sync queue
     *
     * @param deviceId device ID
     * @param request sync item request
     * @return API response
     */
    ApiResponse addToSyncQueue(Long deviceId, SyncItemRequest request);

    /**
     * Get pending sync items for device
     *
     * @param deviceId device ID
     * @return list of sync queue items
     */
    List<SyncQueue> getPendingSyncItems(Long deviceId);

    /**
     * Process sync batch
     *
     * @param deviceId device ID
     * @param request sync batch request
     * @return sync status
     */
    SyncStatusResponse processSyncBatch(Long deviceId, SyncBatchRequest request);

    /**
     * Mark sync item as processed
     *
     * @param queueId queue ID
     * @param success success status
     * @return API response
     */
    ApiResponse markSyncItemProcessed(Long queueId, boolean success);

    /**
     * Clean up processed sync items
     *
     * @param hoursToKeep hours to keep processed items
     * @return count of deleted items
     */
    int cleanupProcessedSyncItems(int hoursToKeep);
}