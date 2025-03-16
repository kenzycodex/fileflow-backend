package com.fileflow.service.quota;

import com.fileflow.dto.response.common.ApiResponse;

public interface QuotaService {
    /**
     * Check if user has enough quota and reserve it if available
     *
     * @param userId user ID
     * @param size size to check against the quota
     * @return true if quota available and reserved, false otherwise
     */
    boolean checkAndReserveQuota(Long userId, long size);

    /**
     * Confirm quota usage after successful upload
     *
     * @param userId user ID
     * @param size used storage size
     */
    void confirmQuotaUsage(Long userId, long size);

    /**
     * Cancel reserved quota in case of failed upload
     *
     * @param userId user ID
     * @param size size to release
     */
    void cancelReservedQuota(Long userId, long size);

    /**
     * Update user's storage usage
     *
     * @param userId user ID
     * @param additionalSize additional size used
     */
    void updateStorageUsed(Long userId, long additionalSize);

    /**
     * Release storage after file deletion
     *
     * @param userId user ID
     * @param size size to release
     */
    void releaseStorage(Long userId, long size);

    /**
     * Get user's storage usage information
     *
     * @param userId user ID
     * @return response with usage information
     */
    ApiResponse getStorageUsage(Long userId);

    /**
     * Update user's storage quota
     *
     * @param userId user ID
     * @param newQuota new quota in bytes
     * @return response with updated quota information
     */
    ApiResponse updateQuota(Long userId, long newQuota);
}