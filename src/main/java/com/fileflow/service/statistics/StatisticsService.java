package com.fileflow.service.statistics;

import com.fileflow.dto.response.common.ApiResponse;

import java.time.LocalDate;

public interface StatisticsService {
    /**
     * Get storage statistics for current user
     *
     * @return response with storage statistics
     */
    ApiResponse getStorageStatistics();

    /**
     * Get activity statistics for current user
     *
     * @param startDate optional start date
     * @param endDate optional end date
     * @return response with activity statistics
     */
    ApiResponse getActivityStatistics(LocalDate startDate, LocalDate endDate);

    /**
     * Get file type statistics for current user
     *
     * @return response with file type statistics
     */
    ApiResponse getFileTypeStatistics();

    /**
     * Get sharing statistics for current user
     *
     * @return response with sharing statistics
     */
    ApiResponse getSharingStatistics();

    /**
     * Get system statistics (admin only)
     *
     * @return response with system statistics
     */
    ApiResponse getSystemStatistics();
}