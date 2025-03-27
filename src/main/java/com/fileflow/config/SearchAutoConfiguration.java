package com.fileflow.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;

/**
 * Auto-configuration for search capabilities
 * This class detects if Elasticsearch is available and updates the Spring profiles accordingly
 */
@Configuration
@Slf4j
public class SearchAutoConfiguration {

    /**
     * Detects if Elasticsearch is available and adds the 'elasticsearch' profile if it is
     */
    @Bean
    @ConditionalOnMissingBean
    public ElasticSearchAvailabilityChecker elasticSearchAvailabilityChecker(Environment environment) {
        return new ElasticSearchAvailabilityChecker(environment);
    }

    /**
     * Class to check Elasticsearch availability and update Spring profiles
     */
    public static class ElasticSearchAvailabilityChecker {
        private final Environment environment;
        private boolean elasticsearchAvailable = false;

        public ElasticSearchAvailabilityChecker(Environment environment) {
            this.environment = environment;
            checkElasticsearchAvailability();
        }

        private void checkElasticsearchAvailability() {
            try {
                // Check if Elasticsearch client class is on the classpath
                Class.forName("org.elasticsearch.client.RestHighLevelClient");

                // Check if Elasticsearch is already in the active profiles
                if (Arrays.asList(environment.getActiveProfiles()).contains("elasticsearch")) {
                    elasticsearchAvailable = true;
                    log.info("Elasticsearch profile is active");
                    return;
                }

                // Try to connect to Elasticsearch
                try {
                    String host = environment.getProperty("elasticsearch.host", "localhost");
                    int port = environment.getProperty("elasticsearch.port", Integer.class, 9200);

                    // Simple socket connection test
                    java.net.Socket socket = new java.net.Socket();
                    socket.connect(new java.net.InetSocketAddress(host, port), 1000);
                    socket.close();

                    log.info("Elasticsearch is available at {}:{}", host, port);
                    elasticsearchAvailable = true;

                    // Set the profile programmatically (for informational purposes only - profiles should be set at startup)
                    log.info("For best results, add 'elasticsearch' to your active profiles on next startup");
                } catch (Exception e) {
                    log.info("Elasticsearch is not available: {}", e.getMessage());
                    log.debug("Connection details", e);
                }
            } catch (ClassNotFoundException e) {
                log.info("Elasticsearch client is not on the classpath, search will use basic database queries");
            }
        }

        public boolean isElasticsearchAvailable() {
            return elasticsearchAvailable;
        }
    }
}