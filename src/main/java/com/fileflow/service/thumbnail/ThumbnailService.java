package com.fileflow.service.thumbnail;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ThumbnailService {
    /**
     * Generate thumbnail for file
     *
     * @param fileId ID of the file to generate thumbnail for
     * @return true if thumbnail generated successfully, false otherwise
     */
    boolean generateThumbnail(Long fileId);

    /**
     * Generate thumbnail from multipart file
     *
     * @param file multipart file
     * @param fileId ID of the file to associate thumbnail with
     * @return thumbnail path or null if generation failed
     * @throws IOException if error occurs during thumbnail generation
     */
    String generateThumbnail(MultipartFile file, Long fileId) throws IOException;

    /**
     * Get thumbnail URL for file
     *
     * @param fileId ID of the file to get thumbnail for
     * @return thumbnail URL or null if not available
     */
    String getThumbnailUrl(Long fileId);

    /**
     * Check if file has thumbnail
     *
     * @param fileId ID of the file to check
     * @return true if file has thumbnail, false otherwise
     */
    boolean hasThumbnail(Long fileId);

    /**
     * Delete thumbnail for file
     *
     * @param fileId ID of the file to delete thumbnail for
     * @return true if thumbnail deleted successfully, false otherwise
     */
    boolean deleteThumbnail(Long fileId);

    /**
     * Generate thumbnails for files that don't have them yet
     *
     * @param batchSize maximum number of files to process
     * @return number of thumbnails generated
     */
    int batchGenerateThumbnails(int batchSize);
}