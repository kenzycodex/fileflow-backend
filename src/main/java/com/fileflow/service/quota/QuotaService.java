package com.fileflow.service.quota;

import com.fileflow.dto.response.common.ApiResponse;

public interface QuotaService {
    /**
     * Check if a user has enough quota and reserve it
     *
     * @param userId User ID
     * @param size Size to reserve
     * @return true if quota is available and reserved
     */
    boolean checkAndReserveQuota(Long userId, long size);

    /**
     * Confirm quota usage after file is successfully stored
     *
     * @param userId User ID
     * @param size Size used
     */
    void confirmQuotaUsage(Long userId, long size);

    /**
     * Cancel reserved quota
     *
     * @param userId User ID
     * @param size Size to cancel
     */
    void cancelReservedQuota(Long userId, long size);

    /**
     * Release previously reserved quota
     *
     * @param userId User ID
     * @param size Size to release
     */
    void releaseQuotaReservation(Long userId, long size);

    /**
     * Update storage used by a user
     *
     * @param userId User ID
     * @param additionalSize Additional size used
     */
    void updateStorageUsed(Long userId, long additionalSize);

    /**
     * Release storage when a file is deleted
     *
     * @param userId User ID
     * @param size Size to release
     */
    void releaseStorage(Long userId, long size);

    /**
     * Get storage usage for a user
     *
     * @param userId User ID
     * @return API response with storage usage information
     */
    ApiResponse getStorageUsage(Long userId);

    /**
     * Update quota for a user
     *
     * @param userId User ID
     * @param newQuota New quota
     * @return API response
     */
    ApiResponse updateQuota(Long userId, long newQuota);
}