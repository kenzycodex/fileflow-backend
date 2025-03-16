package com.fileflow.service.trash;

import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.common.SearchResponse;

public interface TrashService {
    /**
     * Get items in trash
     *
     * @param page page number
     * @param size page size
     * @return trash items
     */
    SearchResponse getTrashItems(int page, int size);

    /**
     * Empty trash (permanently delete all items in trash)
     *
     * @return response with deletion information
     */
    ApiResponse emptyTrash();

    /**
     * Restore all items from trash
     *
     * @return response with restoration information
     */
    ApiResponse restoreAllFromTrash();

    /**
     * Get trash information (count of items, total size, etc.)
     *
     * @return response with trash information
     */
    ApiResponse getTrashInfo();
}