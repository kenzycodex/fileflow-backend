package com.fileflow.service.statistics;

import com.fileflow.dto.response.statistics.StorageStatistics;
import com.fileflow.dto.response.statistics.ActivityStatistics;
import com.fileflow.dto.response.statistics.FileTypeStatistics;
import com.fileflow.dto.response.statistics.SystemStatistics;

import java.time.LocalDate;

public interface StatisticsService {
    /**
     * Get storage statistics for current user
     *
     * @return storage statistics
     */
    StorageStatistics getStorageStatistics();

    /**
     * Get storage statistics for specific user
     *
     * @param userId user ID
     * @return storage statistics
     */
    StorageStatistics getStorageStatisticsForUser(Long userId);

    /**
     * Get activity statistics for current user
     *
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @return activity statistics
     */
    ActivityStatistics getActivityStatistics(LocalDate startDate, LocalDate endDate);

    /**
     * Get file type statistics for current user
     *
     * @return file type statistics
     */
    FileTypeStatistics getFileTypeStatistics();

    /**
     * Get system statistics (admin only)
     *
     * @return system statistics
     */
    SystemStatistics getSystemStatistics();
}