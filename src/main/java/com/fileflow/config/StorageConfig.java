package com.fileflow.config;

import com.fileflow.exception.StorageException;
import io.minio.MinioClient;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Configuration class for file storage settings
 */
@Configuration
@ConfigurationProperties(prefix = "fileflow.storage")
@Getter
@Setter
@Slf4j
public class StorageConfig {

    /**
     * Base storage location
     */
    private String location = "fileflow-storage";

    /**
     * Max allowed file size for uploads (as string, e.g., "100MB")
     */
    private String maxFileSize = "100MB";

    /**
     * Chunk size for chunked uploads (as string, e.g., "5MB")
     */
    private String chunkSize = "5MB";

    /**
     * Number of days to keep files in trash
     */
    private int trashRetentionDays = 30;

    /**
     * Max versions to keep per file
     */
    private int maxVersionsPerFile = 10;

    /**
     * Expiry time (minutes) for chunked uploads
     */
    private int chunkExpiryMinutes = 60;

    /**
     * Whether to use hash-based deduplication
     */
    private boolean enableDeduplication = true;

    /**
     * Whether to generate previews automatically
     */
    private boolean generatePreviews = true;

    /**
     * Whether to generate thumbnails automatically
     */
    private boolean generateThumbnails = true;

    @Value("${app.storage.strategy:local}")
    private String storageStrategy;

    @Value("${app.minio.endpoint:http://localhost:9000}")
    private String minioEndpoint;

    @Value("${app.minio.access-key:minioadmin}")
    private String minioAccessKey;

    @Value("${app.minio.secret-key:minioadmin}")
    private String minioSecretKey;

    @Value("${app.minio.bucket:fileflow}")
    private String minioBucket;

    /**
     * Convert string size (e.g., "5MB") to bytes
     * @param sizeStr size as string with unit (KB, MB, GB)
     * @return size in bytes
     */
    public long parseSize(String sizeStr) {
        if (sizeStr == null || sizeStr.isEmpty()) {
            return 0;
        }

        Pattern pattern = Pattern.compile("(\\d+)([KMG]B)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sizeStr.trim());

        if (matcher.matches()) {
            long size = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2).toUpperCase();

            switch (unit) {
                case "KB":
                    return size * 1024;
                case "MB":
                    return size * 1024 * 1024;
                case "GB":
                    return size * 1024 * 1024 * 1024;
                default:
                    return size;
            }
        }

        // If no unit specified, assume bytes
        return Long.parseLong(sizeStr);
    }

    /**
     * Get chunk size in bytes
     * @return chunk size in bytes
     */
    public int getChunkSizeBytes() {
        return (int) parseSize(chunkSize);
    }

    /**
     * Get max file size in bytes
     * @return max file size in bytes
     */
    public long getMaxFileSizeBytes() {
        return parseSize(maxFileSize);
    }

    /**
     * File storage location as a Path
     */
    @Bean
    @Profile("!minio")
    public Path fileStorageLocation() {
        Path fileStoragePath;

        // Handle both absolute and relative paths
        if (Paths.get(location).isAbsolute()) {
            fileStoragePath = Paths.get(location);
        } else {
            fileStoragePath = Paths.get(System.getProperty("user.dir"), location).normalize();
        }

        log.info("File storage location configured at: {}", fileStoragePath.toAbsolutePath());

        try {
            // Create directories if they don't exist
            if (!Files.exists(fileStoragePath)) {
                Files.createDirectories(fileStoragePath);
                log.info("Created storage directory at: {}", fileStoragePath.toAbsolutePath());
            }

            // Create required subdirectories for the application
            createSubdirectories(fileStoragePath);

            // Ensure the directory is writable
            if (!Files.isWritable(fileStoragePath)) {
                throw new StorageException("Storage directory is not writable: " + fileStoragePath.toAbsolutePath());
            }

            return fileStoragePath;
        } catch (IOException e) {
            throw new StorageException("Could not create or access storage directory: " + fileStoragePath.toAbsolutePath(), e);
        }
    }

    /**
     * Create necessary subdirectories for different storage needs
     */
    private void createSubdirectories(Path basePath) throws IOException {
        // Create subdirectories for different types of content
        String[] subdirs = {"previews", "thumbnails", "temp", "versions"};

        for (String dir : subdirs) {
            Path subdir = basePath.resolve(dir);
            if (!Files.exists(subdir)) {
                Files.createDirectories(subdir);
                log.info("Created subdirectory: {}", subdir.toAbsolutePath());
            }
        }
    }

    /**
     * Configure MinIO client
     */
    @Bean
    @Profile("minio")
    public MinioClient minioClient() {
        log.info("Configuring MinIO client with endpoint: {}", minioEndpoint);
        return MinioClient.builder()
                .endpoint(minioEndpoint)
                .credentials(minioAccessKey, minioSecretKey)
                .build();
    }
}