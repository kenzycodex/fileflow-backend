package com.fileflow.config;

import com.fileflow.service.search.SearchService;
import com.fileflow.service.search.SearchServiceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for search strategy selection
 */
@Configuration
public class SearchStrategyConfig {

    /**
     * Configures the search service based on the active profiles
     * Note: We removed the @Primary annotation since SearchServiceImpl already has it
     */
    @Bean(name = "searchService")
    public SearchService searchService(SearchServiceFactory searchServiceFactory) {
        return searchServiceFactory.getSearchService();
    }
}