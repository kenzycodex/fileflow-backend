package com.fileflow.service.thumbnail;

import com.fileflow.model.File;

import java.util.List;

/**
 * Service interface for thumbnail operations
 */
public interface ThumbnailService {

    /**
     * Find files without thumbnails for a specific user
     *
     * @param userId ID of the user
     * @return List of files without thumbnails
     */
    List<File> findFilesWithoutThumbnails(Long userId);

    /**
     * Generate a thumbnail for a specific file
     *
     * @param fileId ID of the file to generate thumbnail for
     */
    void generateThumbnailForFile(Long fileId);

    /**
     * Generate thumbnails for all files that don't have them yet
     *
     * @param userId ID of the user whose files need thumbnails
     */
    void generateMissingThumbnails(Long userId);

    /**
     * Clear a thumbnail for a specific file
     *
     * @param fileId ID of the file to clear thumbnail for
     */
    void clearThumbnail(Long fileId);
}