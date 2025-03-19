package com.fileflow.service.storage;

import com.fileflow.config.StorageConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Factory for creating the appropriate storage service based on configuration
 */
@Component
public class StorageServiceFactory {

    private final LocalEnhancedStorageService localStorageService;
    private final MinioEnhancedStorageService minioStorageService;  // Will be null if the minio profile is not active
    private final String storageStrategy;

    @Autowired
    public StorageServiceFactory(
            LocalEnhancedStorageService localStorageService,
            @Autowired(required = false) @Lazy MinioEnhancedStorageService minioStorageService,
            StorageConfig storageConfig) {
        this.localStorageService = localStorageService;
        this.minioStorageService = minioStorageService;
        this.storageStrategy = storageConfig.getStorageStrategy();
    }

    /**
     * Get the appropriate storage service based on configuration
     */
    public EnhancedStorageService getStorageService() {
        if ("minio".equalsIgnoreCase(storageStrategy) && minioStorageService != null) {
            return minioStorageService;
        }

        // Default to local storage
        return localStorageService;
    }
}