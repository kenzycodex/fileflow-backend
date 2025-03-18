package com.fileflow.service.search;

import com.fileflow.dto.response.common.SearchResponse;
import com.fileflow.dto.response.file.FileResponse;
import com.fileflow.model.File;
import com.fileflow.model.search.FileSearchDocument;
import com.fileflow.repository.FileRepository;
import com.fileflow.repository.search.FileSearchRepository;
import com.fileflow.service.storage.EnhancedStorageService;
import com.fileflow.service.storage.StorageServiceFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of ElasticsearchSearchService that uses Elasticsearch for search
 */
@Service
@Profile("elasticsearch")
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchSearchServiceImpl implements ElasticsearchSearchService {

    private final RestHighLevelClient elasticsearchClient;
    private final FileSearchRepository fileSearchRepository;
    private final FileRepository fileRepository;
    private final StorageServiceFactory storageServiceFactory;

    private static final String INDEX_NAME = "files";

    @Override
    public void indexFile(File file) {
        try {
            // Extract text from file if possible
            String content = "";
            try {
                EnhancedStorageService storageService = storageServiceFactory.getStorageService();
                content = storageService.extractText(file.getStoragePath(), file.getMimeType());
            } catch (Exception e) {
                log.warn("Failed to extract text from file: {}", file.getId(), e);
            }

            // Create search document
            FileSearchDocument document = FileSearchDocument.fromEntity(file, content);

            // Save to Elasticsearch
            fileSearchRepository.save(document);

            log.debug("Indexed file in Elasticsearch: {}", file.getId());
        } catch (Exception e) {
            log.error("Failed to index file in Elasticsearch: {}", file.getId(), e);
        }
    }

    @Override
    public void removeFileIndex(Long fileId) {
        try {
            fileSearchRepository.deleteById(fileId);
            log.debug("Removed file from Elasticsearch index: {}", fileId);
        } catch (Exception e) {
            log.error("Failed to remove file from Elasticsearch index: {}", fileId, e);
        }
    }

    @Override
    public SearchResponse fullSearch(String query, Long userId, int page, int size) {
        try {
            // Search across all fields including content
            Pageable pageable = PageRequest.of(page, size);
            Page<FileSearchDocument> searchResults;

            if (query.trim().contains(" ")) {
                // Multi-word query - exact phrase match is more relevant
                searchResults = fileSearchRepository.search(query, userId, pageable);
            } else {
                // Single word query - fuzzy search might be more helpful
                searchResults = performFuzzySearch(query, userId, pageable);
            }

            // Convert to response
            List<FileResponse> fileResponses = convertToFileResponses(searchResults.getContent());

            return SearchResponse.builder()
                    .files(fileResponses)
                    .folders(Collections.emptyList()) // Elasticsearch doesn't index folders currently
                    .page(page)
                    .size(size)
                    .totalElements(searchResults.getTotalElements())
                    .totalPages(searchResults.getTotalPages())
                    .hasMore(page < searchResults.getTotalPages() - 1)
                    .query(query)
                    .build();
        } catch (Exception e) {
            log.error("Elasticsearch full search failed: {}", query, e);
            return SearchResponse.builder()
                    .files(Collections.emptyList())
                    .folders(Collections.emptyList())
                    .page(page)
                    .size(size)
                    .totalElements(0)
                    .totalPages(0)
                    .hasMore(false)
                    .query(query)
                    .build();
        }
    }

    @Override
    public SearchResponse searchFiles(String query, Long userId, int page, int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<FileSearchDocument> searchResults = fileSearchRepository.search(query, userId, pageable);

            List<FileResponse> fileResponses = convertToFileResponses(searchResults.getContent());

            return SearchResponse.builder()
                    .files(fileResponses)
                    .folders(Collections.emptyList())
                    .page(page)
                    .size(size)
                    .totalElements(searchResults.getTotalElements())
                    .totalPages(searchResults.getTotalPages())
                    .hasMore(page < searchResults.getTotalPages() - 1)
                    .query(query)
                    .build();
        } catch (Exception e) {
            log.error("Elasticsearch file search failed: {}", query, e);
            return SearchResponse.builder()
                    .files(Collections.emptyList())
                    .folders(Collections.emptyList())
                    .page(page)
                    .size(size)
                    .totalElements(0)
                    .totalPages(0)
                    .hasMore(false)
                    .query(query)
                    .build();
        }
    }

    @Override
    public SearchResponse searchByContent(String query, Long userId, int page, int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<FileSearchDocument> searchResults = fileSearchRepository.searchContent(query, userId, pageable);

            List<FileResponse> fileResponses = convertToFileResponses(searchResults.getContent());

            return SearchResponse.builder()
                    .files(fileResponses)
                    .folders(Collections.emptyList())
                    .page(page)
                    .size(size)
                    .totalElements(searchResults.getTotalElements())
                    .totalPages(searchResults.getTotalPages())
                    .hasMore(page < searchResults.getTotalPages() - 1)
                    .query(query)
                    .build();
        } catch (Exception e) {
            log.error("Elasticsearch content search failed: {}", query, e);
            return SearchResponse.builder()
                    .files(Collections.emptyList())
                    .folders(Collections.emptyList())
                    .page(page)
                    .size(size)
                    .totalElements(0)
                    .totalPages(0)
                    .hasMore(false)
                    .query(query)
                    .build();
        }
    }

    @Override
    public SearchResponse searchByFileType(String fileType, String query, Long userId, int page, int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<FileSearchDocument> searchResults;

            if (StringUtils.hasText(query)) {
                // Search by file type and query
                BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                        .must(QueryBuilders.termQuery("fileType", fileType))
                        .must(QueryBuilders.multiMatchQuery(query, "filename", "content"))
                        .filter(QueryBuilders.termQuery("userId", userId))
                        .filter(QueryBuilders.termQuery("isDeleted", false));

                // Use custom query
                searchResults = executeCustomQuery(boolQuery, pageable);
            } else {
                // Just search by file type
                searchResults = fileSearchRepository.findByUserIdAndFileTypeAndIsDeletedFalse(userId, fileType, pageable);
            }

            List<FileResponse> fileResponses = convertToFileResponses(searchResults.getContent());

            return SearchResponse.builder()
                    .files(fileResponses)
                    .folders(Collections.emptyList())
                    .page(page)
                    .size(size)
                    .totalElements(searchResults.getTotalElements())
                    .totalPages(searchResults.getTotalPages())
                    .hasMore(page < searchResults.getTotalPages() - 1)
                    .query(query)
                    .build();
        } catch (Exception e) {
            log.error("Elasticsearch file type search failed: {} - {}", fileType, query, e);
            return SearchResponse.builder()
                    .files(Collections.emptyList())
                    .folders(Collections.emptyList())
                    .page(page)
                    .size(size)
                    .totalElements(0)
                    .totalPages(0)
                    .hasMore(false)
                    .query(query)
                    .build();
        }
    }

    @Override
    public SearchResponse searchByTag(String tag, Long userId, int page, int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<FileSearchDocument> searchResults = fileSearchRepository.findByUserIdAndTagsContainingAndIsDeletedFalse(
                    userId, tag, pageable);

            List<FileResponse> fileResponses = convertToFileResponses(searchResults.getContent());

            return SearchResponse.builder()
                    .files(fileResponses)
                    .folders(Collections.emptyList())
                    .page(page)
                    .size(size)
                    .totalElements(searchResults.getTotalElements())
                    .totalPages(searchResults.getTotalPages())
                    .hasMore(page < searchResults.getTotalPages() - 1)
                    .query(tag)
                    .build();
        } catch (Exception e) {
            log.error("Elasticsearch tag search failed: {}", tag, e);
            return SearchResponse.builder()
                    .files(Collections.emptyList())
                    .folders(Collections.emptyList())
                    .page(page)
                    .size(size)
                    .totalElements(0)
                    .totalPages(0)
                    .hasMore(false)
                    .query(tag)
                    .build();
        }
    }

    @Override
    public List<FileSearchDocument> searchFileDocuments(String query, Long userId, int page, int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<FileSearchDocument> searchResults = fileSearchRepository.search(query, userId, pageable);
            return searchResults.getContent();
        } catch (Exception e) {
            log.error("Elasticsearch document search failed: {}", query, e);
            return new ArrayList<>();
        }
    }

    @Override
    public void reindexUserFiles(Long userId) {
        log.info("Starting reindexing for user: {}", userId);
        try {
            // Delete all existing documents for this user
            BoolQueryBuilder deleteQuery = QueryBuilders.boolQuery()
                    .must(QueryBuilders.termQuery("userId", userId));

            // Execute delete query
            // This would typically use the DeleteByQuery API, but we'll use the repository for simplicity
            List<FileSearchDocument> existingDocs = findAllByUserId(userId);
            fileSearchRepository.deleteAll(existingDocs);

            // Get all files for the user
            List<File> userFiles = fileRepository.findByUserAndIsDeletedFalse(null); // Replace with actual user entity

            // Reindex all files
            for (File file : userFiles) {
                indexFile(file);
            }

            log.info("Completed reindexing {} files for user: {}", userFiles.size(), userId);
        } catch (Exception e) {
            log.error("Failed to reindex files for user: {}", userId, e);
        }
    }

    // Helper methods

    private List<FileSearchDocument> findAllByUserId(Long userId) {
        // This is a simplified version - in a real implementation, you'd need pagination
        Pageable pageable = PageRequest.of(0, 1000);
        Page<FileSearchDocument> docs = fileSearchRepository.findAll(pageable);
        return docs.getContent().stream()
                .filter(doc -> doc.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    private Page<FileSearchDocument> performFuzzySearch(String query, Long userId, Pageable pageable) {
        // This would typically use a fuzzy query in Elasticsearch
        return fileSearchRepository.search(query, userId, pageable);
    }

    private Page<FileSearchDocument> executeCustomQuery(BoolQueryBuilder query, Pageable pageable) {
        // This would typically execute a custom query against Elasticsearch
        // For now, we'll use a simplified approach with the repository
        return fileSearchRepository.search(query.toString(), null, pageable);
    }

    private List<FileResponse> convertToFileResponses(List<FileSearchDocument> documents) {
        return documents.stream()
                .map(this::documentToFileResponse)
                .collect(Collectors.toList());
    }

    private FileResponse documentToFileResponse(FileSearchDocument document) {
        String downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/files/download/")
                .path(document.getId().toString())
                .toUriString();

        String thumbnailUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/files/thumbnail/")
                .path(document.getId().toString())
                .toUriString();

        return FileResponse.builder()
                .id(document.getId())
                .filename(document.getFilename())
                .originalFilename(document.getOriginalFilename())
                .fileSize(document.getFileSize())
                .fileType(document.getFileType())
                .mimeType(document.getMimeType())
                .parentFolderId(document.getParentFolderId())
                .parentFolderName(document.getParentFolderName())
                .isFavorite(document.isFavorite())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .lastAccessed(document.getLastAccessed())
                .downloadUrl(downloadUrl)
                .thumbnailUrl(thumbnailUrl)
                .owner(document.getUsername())
                .ownerId(document.getUserId())
                .build();
    }
}