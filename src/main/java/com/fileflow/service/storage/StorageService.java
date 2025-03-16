package com.fileflow.service.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

public interface StorageService {
    /**
     * Initialize storage
     */
    void init();

    /**
     * Store a file
     *
     * @param file the file to store
     * @param directory optional subdirectory
     * @return the stored file path/id
     */
    String store(MultipartFile file, String directory) throws IOException;

    /**
     * Store a file with specific name
     *
     * @param file the file to store
     * @param filename the filename to use
     * @param directory optional subdirectory
     * @return the stored file path/id
     */
    String store(MultipartFile file, String filename, String directory) throws IOException;

    /**
     * Load a file as a Resource
     *
     * @param filename the filename to load
     * @return the file resource
     */
    Resource loadAsResource(String filename);

    /**
     * Generate a pre-signed URL for temporary access
     *
     * @param filename the filename
     * @param expiryTimeInMinutes expiry time in minutes
     * @return the pre-signed URL
     */
    String generatePresignedUrl(String filename, int expiryTimeInMinutes);

    /**
     * Delete a file
     *
     * @param filename the filename to delete
     * @return true if deleted successfully
     */
    boolean delete(String filename);

    /**
     * List all files in storage or directory
     *
     * @param directory optional directory to list
     * @return stream of paths
     */
    Stream<Path> loadAll(String directory);

    /**
     * Check if a file exists
     *
     * @param filename the filename to check
     * @return true if exists
     */
    boolean exists(String filename);

    /**
     * Copy a file
     *
     * @param source source filename
     * @param destination destination filename
     * @return true if copied successfully
     */
    boolean copy(String source, String destination);

    /**
     * Move a file
     *
     * @param source source filename
     * @param destination destination filename
     * @return true if moved successfully
     */
    boolean move(String source, String destination);
}