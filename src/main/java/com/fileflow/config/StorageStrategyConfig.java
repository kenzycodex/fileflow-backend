package com.fileflow.config;

import com.fileflow.service.storage.EnhancedStorageService;
import com.fileflow.service.storage.LocalEnhancedStorageService;
import com.fileflow.service.storage.MinioEnhancedStorageService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration for storage strategy selection
 */
@Configuration
public class StorageStrategyConfig {

    /**
     * Configures the storage service based on the specified strategy
     */
    @Bean
    @Primary
    public EnhancedStorageService storageService(
            StorageConfig storageConfig,
            LocalEnhancedStorageService localStorageService,
            MinioEnhancedStorageService minioStorageService) {

        // Use the strategy specified in configuration
        if ("minio".equalsIgnoreCase(storageConfig.getStorageStrategy())) {
            return minioStorageService;
        }

        // Default to local storage
        return localStorageService;
    }
}