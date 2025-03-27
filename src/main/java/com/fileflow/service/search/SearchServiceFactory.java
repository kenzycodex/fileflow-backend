package com.fileflow.service.search;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Factory for creating the appropriate search service based on active profiles
 */
@Component
public class SearchServiceFactory {

    private final SearchServiceImpl basicSearchService;
    private final ElasticsearchSearchServiceImpl elasticsearchSearchService; // Will be null if elasticsearch profile isn't active
    private final Environment environment;

    @Autowired
    public SearchServiceFactory(
            SearchServiceImpl basicSearchService,
            @Autowired(required = false) @Lazy ElasticsearchSearchServiceImpl elasticsearchSearchService,
            Environment environment) {
        this.basicSearchService = basicSearchService;
        this.elasticsearchSearchService = elasticsearchSearchService;
        this.environment = environment;
    }

    /**
     * Get the appropriate search service based on active profiles
     */
    public SearchService getSearchService() {
        boolean elasticsearchEnabled = Arrays.asList(environment.getActiveProfiles()).contains("elasticsearch");

        if (elasticsearchEnabled && elasticsearchSearchService != null) {
            return basicSearchService; // Return the primary service which has elastic capabilities injected
        }

        // Default to basic search
        return basicSearchService;
    }
}