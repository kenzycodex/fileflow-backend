package com.fileflow.config.test;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/**
 * A fixed environment implementation for tests
 * This implementation properly handles the Boolean return types needed by ValidationAutoConfiguration
 */
public class FixedEnvironment implements Environment {

    @Override
    public String getProperty(String key) {
        return null;
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        return defaultValue;
    }

    @Override
    public <T> T getProperty(String key, Class<T> targetType) {
        return getProperty(key, targetType, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        if (targetType == Boolean.class) {
            return (T) Boolean.FALSE; // Return actual Boolean instead of null
        }
        return defaultValue;
    }

    @Override
    public String getRequiredProperty(String key) {
        return "";
    }

    @Override
    public <T> T getRequiredProperty(String key, Class<T> targetType) {
        return getProperty(key, targetType);
    }

    @Override
    public String[] getActiveProfiles() {
        return new String[]{"test"};
    }

    @Override
    public String[] getDefaultProfiles() {
        return new String[0];
    }

    @Override
    public boolean acceptsProfiles(String... profiles) {
        for (String profile : profiles) {
            if (profile.equals("test")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean acceptsProfiles(Profiles profiles) {
        return profiles.matches(this::acceptsProfiles);
    }

    @Override
    public boolean containsProperty(String key) {
        return false;
    }

    @Override
    public String resolvePlaceholders(String text) {
        return text;
    }

    @Override
    public String resolveRequiredPlaceholders(String text) {
        return text;
    }
}