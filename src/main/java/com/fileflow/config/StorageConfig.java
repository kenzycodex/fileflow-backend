package com.fileflow.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.storage")
public class StorageConfig {
    private String location;
    private String maxFileSize;
    private String allowedContentTypes;
    private String strategy; // "local" or "minio"

    @Bean
    public Path fileStorageLocation() {
        return Paths.get(location)
                .toAbsolutePath()
                .normalize();
    }
    
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(System.getProperty("app.minio.endpoint", "http://localhost:9000"))
                .credentials(
                        System.getProperty("app.minio.access-key", "minioadmin"),
                        System.getProperty("app.minio.secret-key", "minioadmin")
                )
                .build();
    }
}