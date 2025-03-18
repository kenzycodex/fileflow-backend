package com.fileflow.config;

import com.fileflow.exception.StorageException;
import io.minio.MinioClient;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration class for file storage settings
 */
@Configuration
@ConfigurationProperties(prefix = "fileflow.storage")
@Getter
@Setter
public class StorageConfig {

    /**
     * Base storage location
     */
    private String location = "storage";

    /**
     * Max allowed file size for uploads
     */
    private long maxFileSize = 100 * 1024 * 1024; // 100MB

    /**
     * Chunk size for chunked uploads
     */
    private int chunkSize = 5 * 1024 * 1024; // 5MB

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
     * File storage location as a Path
     */
    @Bean
    @Profile("!minio")
    public Path fileStorageLocation() {
        Path fileStoragePath = Paths.get(location).toAbsolutePath().normalize();

        try {
            // Create directories if they don't exist
            if (!Files.exists(fileStoragePath)) {
                Files.createDirectories(fileStoragePath);
            }
            return fileStoragePath;
        } catch (IOException e) {
            throw new StorageException("Could not create storage directory", e);
        }
    }

    /**
     * Configure MinIO client
     */
    @Bean
    @Profile("minio")
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioEndpoint)
                .credentials(minioAccessKey, minioSecretKey)
                .build();
    }
}