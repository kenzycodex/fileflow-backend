package com.fileflow.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        // Combine existing caches with new ones needed for rate limiting
        List<String> cacheNames = Arrays.asList(
                // Existing caches
                "files",
                "folders",
                "users",
                "shares",
                "userQuota",

                // New caches for rate limiting and security
                "rateLimitCache",
                "rateLimitExpireCache"
        );

        return new ConcurrentMapCacheManager(cacheNames.toArray(new String[0]));
    }
}