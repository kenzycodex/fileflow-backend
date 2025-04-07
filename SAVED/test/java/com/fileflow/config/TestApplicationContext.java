package com.fileflow.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.mockito.Mockito;
import com.fileflow.service.config.EnvPropertyService;

/**
 * Test configuration to provide necessary beans for the test environment
 */
@TestConfiguration
public class TestApplicationContext {

    @Bean
    @Primary
    public DotenvConfig dotenvConfig() {
        return new DotenvConfig();
    }

    @Bean
    @Primary
    public EnvPropertyService envPropertyService(DotenvConfig dotenvConfig, Environment environment) {
        return new EnvPropertyService(dotenvConfig, environment);
    }
}