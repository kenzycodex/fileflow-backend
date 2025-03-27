package com.fileflow.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;

/**
 * Configuration for MinIO client
 */
@Configuration
@Profile("minio")
@Slf4j
public class MinioConfig {

    @Value("${app.minio.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${app.minio.access-key:minioadmin}")
    private String accessKey;

    @Value("${app.minio.secret-key:minioadmin}")
    private String secretKey;

    @Value("${app.minio.bucket:fileflow}")
    private String bucketName;

    /**
     * Creates and configures MinIO client
     * Note: MinIO's API endpoint is 9000, while the web console is at 9001
     */
    @Bean
    public MinioClient minioClient() {
        log.info("Configuring MinIO client with endpoint: {}", endpoint);
        // Important: The MinIO API uses port 9000, not 9001 (which is for the web console)
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    /**
     * Initialize MinIO bucket if it doesn't exist
     */
    @PostConstruct
    public void initializeBucket() {
        try {
            MinioClient client = minioClient();
            boolean bucketExists = client.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());

            if (!bucketExists) {
                client.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
                log.info("Created MinIO bucket: {}", bucketName);
            } else {
                log.info("Using existing MinIO bucket: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("Failed to initialize MinIO bucket", e);
            log.error("MinIO may not be available. If using Docker, make sure the container is running.");
            log.error("Note: MinIO API uses port 9000, web console uses port 9001.");
            // Don't throw exception here to allow application to start without MinIO
        }
    }
}