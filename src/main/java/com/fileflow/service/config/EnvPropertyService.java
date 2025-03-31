package com.fileflow.service.config;

import com.fileflow.config.DotenvConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * Service to load properties from environment variables or .env file
 * This provides a unified way to access configuration values
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EnvPropertyService {

    private final DotenvConfig dotenvConfig;
    private final Environment environment;

    /**
     * Get a property value from environment or application properties
     * Priority: 1. System environment variables, 2. .env file, 3. application.properties
     *
     * @param key          The property key to look up
     * @param defaultValue Default value if not found
     * @return The property value or default if not found
     */
    public String getProperty(String key, String defaultValue) {
        // First try system environment variable
        String value = System.getenv(key);

        // Then try dotenv
        if (value == null || value.isEmpty()) {
            value = dotenvConfig.get(key);
        }

        // Finally try Spring properties
        if (value == null || value.isEmpty()) {
            value = environment.getProperty(key);
        }

        // Return value or default
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    /**
     * Get a property value from environment or application properties
     *
     * @param key The property key to look up
     * @return The property value or null if not found
     */
    public String getProperty(String key) {
        return getProperty(key, null);
    }

    /**
     * Get a property as boolean
     *
     * @param key The property key
     * @param defaultValue Default value if not found
     * @return The boolean value
     */
    public boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = getProperty(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    /**
     * Get a property as integer
     *
     * @param key The property key
     * @param defaultValue Default value if not found or not a valid integer
     * @return The integer value
     */
    public int getIntProperty(String key, int defaultValue) {
        String value = getProperty(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("Invalid integer value for property {}: {}", key, value);
            return defaultValue;
        }
    }

    /**
     * Get a property as long
     *
     * @param key The property key
     * @param defaultValue Default value if not found or not a valid long
     * @return The long value
     */
    public long getLongProperty(String key, long defaultValue) {
        String value = getProperty(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.warn("Invalid long value for property {}: {}", key, value);
            return defaultValue;
        }
    }
}