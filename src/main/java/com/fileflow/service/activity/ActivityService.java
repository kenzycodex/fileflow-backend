package com.fileflow.service.activity;

import com.fileflow.dto.response.common.PagedResponse;
import com.fileflow.model.Activity;

import java.util.List;

public interface ActivityService {
    /**
     * Log user activity
     *
     * @param userId user ID
     * @param activityType type of activity (e.g., UPLOAD, DOWNLOAD)
     * @param itemType type of item (e.g., FILE, FOLDER)
     * @param itemId ID of the item
     * @param description description of the activity
     * @return the created Activity
     */
    Activity logActivity(Long userId, String activityType, String itemType, Long itemId, String description);

    /**
     * Get activities for the current user
     *
     * @param page page number
     * @param size page size
     * @return paged list of activities
     */
    PagedResponse<Activity> getCurrentUserActivities(int page, int size);

    /**
     * Get activities for a specific user
     *
     * @param userId user ID
     * @param page page number
     * @param size page size
     * @return paged list of activities
     */
    PagedResponse<Activity> getUserActivities(Long userId, int page, int size);

    /**
     * Get recent activities for the current user
     *
     * @param limit maximum number of activities to return
     * @return list of recent activities
     */
    List<Activity> getRecentActivities(int limit);

    /**
     * Delete old activities
     *
     * @param daysToKeep number of days to keep activities
     * @return number of deleted activities
     */
    int deleteOldActivities(int daysToKeep);
}