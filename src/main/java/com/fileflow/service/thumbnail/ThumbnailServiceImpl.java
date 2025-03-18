package com.fileflow.service.thumbnail;

import com.fileflow.model.File;
import com.fileflow.repository.FileRepository;
import com.fileflow.service.storage.EnhancedStorageService;
import com.fileflow.service.storage.StorageServiceFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

/**
 * Service for managing file thumbnails
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ThumbnailServiceImpl implements ThumbnailService {

    private final FileRepository fileRepository;
    private final StorageServiceFactory storageServiceFactory;

    @Override
    @Transactional(readOnly = true)
    public List<File> findFilesWithoutThumbnails(Long userId) {
        // Get the user entity for the query
        // Note: In a real application, you'd want to validate the user exists
        // and has permission to access these files

        // For simplicity, we're assuming the User entity can be constructed with just an ID
        // In a real application, you'd fetch the user from a repository
        com.fileflow.model.User user = new com.fileflow.model.User();
        user.setId(userId);

        return fileRepository.findFilesWithoutThumbnails(user);
    }

    @Override
    @Async
    @Transactional
    public void generateThumbnailForFile(Long fileId) {
        log.info("Generating thumbnail for file ID: {}", fileId);

        // Find the file by ID
        fileRepository.findById(fileId).ifPresent(file -> {
            try {
                // Get the appropriate storage service
                EnhancedStorageService storageService = storageServiceFactory.getStorageService();

                // Check if file has a storage path
                if (file.getStoragePath() == null || file.getStoragePath().isEmpty()) {
                    log.warn("Cannot generate thumbnail for file ID: {} - No storage path", fileId);
                    return;
                }

                // Generate thumbnail
                String thumbnailPath = storageService.generateThumbnail(
                        file.getStoragePath(),
                        file.getFileType(),
                        file.getMimeType()
                );

                // Update file with thumbnail path
                if (thumbnailPath != null && !thumbnailPath.isEmpty()) {
                    file.setThumbnailPath(thumbnailPath);
                    fileRepository.save(file);
                    log.info("Thumbnail generated and saved for file ID: {}", fileId);
                } else {
                    log.warn("Failed to generate thumbnail for file ID: {}", fileId);
                }
            } catch (IOException e) {
                log.error("Error generating thumbnail for file ID: {}", fileId, e);
            }
        });
    }

    @Override
    @Async
    @Transactional
    public void generateMissingThumbnails(Long userId) {
        log.info("Generating missing thumbnails for user ID: {}", userId);

        List<File> filesWithoutThumbnails = findFilesWithoutThumbnails(userId);
        log.info("Found {} files without thumbnails for user ID: {}", filesWithoutThumbnails.size(), userId);

        // Process each file
        for (File file : filesWithoutThumbnails) {
            generateThumbnailForFile(file.getId());
        }
    }

    @Override
    @Transactional
    public void clearThumbnail(Long fileId) {
        log.info("Clearing thumbnail for file ID: {}", fileId);

        fileRepository.findById(fileId).ifPresent(file -> {
            // Check if the file has a thumbnail
            if (file.getThumbnailPath() != null && !file.getThumbnailPath().isEmpty()) {
                // Delete the thumbnail file
                EnhancedStorageService storageService = storageServiceFactory.getStorageService();
                storageService.delete(file.getThumbnailPath());

                // Clear the thumbnail path
                file.setThumbnailPath(null);
                fileRepository.save(file);
                log.info("Thumbnail cleared for file ID: {}", fileId);
            }
        });
    }
}