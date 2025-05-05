package com.fileflow.service.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Base interface for storage services with basic file operations
 */
public interface StorageService {
    /**
     * Initialize the storage service
     */
    void init();

    /**
     * Store a file with a generated filename in the default directory
     *
     * @param file The file to store
     * @param directory The directory to store the file in
     * @return The path of the stored file
     * @throws IOException If an error occurs during storage
     */
    String store(MultipartFile file, String directory) throws IOException;

    /**
     * Store a file with a specific filename in a specified directory
     *
     * @param file The file to store
     * @param filename The name to use for the file
     * @param directory The directory to store the file in
     * @return The path of the stored file
     * @throws IOException If an error occurs during storage
     */
    String store(MultipartFile file, String filename, String directory) throws IOException;

    /**
     * Store a file from an InputStream with specific filename in a specified directory
     *
     * @param inputStream The input stream of the file
     * @param contentLength The length of the content in bytes
     * @param filename The name to use for the file
     * @param directory The directory to store the file in
     * @return The path of the stored file
     * @throws IOException If an error occurs during storage
     */
    String store(InputStream inputStream, int contentLength, String filename, String directory) throws IOException;

    /**
     * Load a file as a resource
     *
     * @param filename The name of the file to load
     * @return The file as a Resource
     */
    Resource loadAsResource(String filename);

    /**
     * Get input stream for a file
     *
     * @param filename The name of the file
     * @return InputStream for the file
     * @throws IOException If an error occurs
     */
    InputStream getInputStream(String filename) throws IOException;

    /**
     * Generate a pre-signed URL for accessing a file
     *
     * @param filename The name of the file
     * @param expiryTimeInMinutes Expiration time for the URL
     * @return Pre-signed URL
     */
    String generatePresignedUrl(String filename, int expiryTimeInMinutes);

    /**
     * Delete a file
     *
     * @param filename The name of the file to delete
     * @return true if deletion was successful, false otherwise
     */
    boolean delete(String filename);

    /**
     * List all files in a directory
     *
     * @param directory The directory to list files from
     * @return Stream of file paths
     */
    Stream<Path> loadAll(String directory);

    /**
     * Check if a file exists
     *
     * @param filename The name of the file to check
     * @return true if the file exists, false otherwise
     */
    boolean exists(String filename);

    /**
     * Copy a file
     *
     * @param source The source file path
     * @param destination The destination file path
     * @return true if copying was successful, false otherwise
     */
    boolean copy(String source, String destination);

    /**
     * Move a file
     *
     * @param source The source file path
     * @param destination The destination file path
     * @return true if moving was successful, false otherwise
     */
    boolean move(String source, String destination);
}