package com.fileflow.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration for DotenvConfig
 * Provides a mock DotenvConfig bean for tests
 */
@TestConfiguration
public class TestDotenvConfig {

    @Bean
    @Primary
    public DotenvConfig dotenvConfig() {
        return new DotenvConfig();
    }
}