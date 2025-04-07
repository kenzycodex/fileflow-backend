package com.fileflow.config;

import com.fileflow.dto.response.common.SearchResponse;
import com.fileflow.model.File;
import com.fileflow.service.search.SearchService;
import com.fileflow.service.search.SearchServiceImpl;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * Test configuration for search service
 * This configuration replaces the search services with mocks for integration tests
 */
@TestConfiguration
public class TestSearchConfig {

    /**
     * Creates a mock SearchService bean for tests
     */
    @Bean
    @Primary
    public SearchService mockSearchService() {
        // Return a mock implementation for tests
        return new MockSearchService();
    }

    /**
     * Simple mock implementation of SearchService that does nothing
     * This avoids any Elasticsearch dependencies in tests
     */
    private static class MockSearchService implements SearchService {
        @Override
        public SearchResponse search(String query, int page, int size) {
            return SearchResponse.builder().build();
        }

        @Override
        public SearchResponse getRecentItems(int page, int size) {
            return SearchResponse.builder().build();
        }

        @Override
        public SearchResponse getFavoriteItems(int page, int size) {
            return SearchResponse.builder().build();
        }

        @Override
        public SearchResponse searchFiles(String query, int page, int size) {
            return SearchResponse.builder().build();
        }

        @Override
        public SearchResponse searchFolders(String query, int page, int size) {
            return SearchResponse.builder().build();
        }

        @Override
        public SearchResponse searchByFileType(String fileType, String query, int page, int size) {
            return SearchResponse.builder().build();
        }

        @Override
        public SearchResponse searchTrash(String query, int page, int size) {
            return SearchResponse.builder().build();
        }

        @Override
        public SearchResponse searchFileContents(String query, int page, int size) {
            return SearchResponse.builder().build();
        }

        @Override
        public SearchResponse searchByTag(String tag, int page, int size) {
            return SearchResponse.builder().build();
        }

        @Override
        public void indexFile(File file) {
            // Do nothing
        }

        @Override
        public void removeFileFromIndex(Long fileId) {
            // Do nothing
        }
    }
}