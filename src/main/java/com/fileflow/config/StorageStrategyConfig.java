package com.fileflow.config;

import com.fileflow.service.storage.EnhancedStorageService;
import com.fileflow.service.storage.LocalEnhancedStorageService;
import com.fileflow.service.storage.StorageServiceFactory;
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
    public EnhancedStorageService storageService(StorageServiceFactory storageServiceFactory) {
        return storageServiceFactory.getStorageService();
    }
}