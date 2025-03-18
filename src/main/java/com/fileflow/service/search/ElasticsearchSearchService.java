package com.fileflow.service.search;

import com.fileflow.dto.response.common.SearchResponse;
import com.fileflow.model.File;
import com.fileflow.model.search.FileSearchDocument;

import java.util.List;

/**
 * Interface for Elasticsearch-specific search operations
 */
public interface ElasticsearchSearchService {

    /**
     * Index a file for search
     *
     * @param file the file to index
     */
    void indexFile(File file);

    /**
     * Remove a file from the search index
     *
     * @param fileId the ID of the file to remove
     */
    void removeFileIndex(Long fileId);

    /**
     * Comprehensive search across filenames, content, and metadata
     *
     * @param query the search query
     * @param userId the user ID
     * @param page the page number
     * @param size the page size
     * @return search response with results
     */
    SearchResponse fullSearch(String query, Long userId, int page, int size);

    /**
     * Search files by filename and metadata
     *
     * @param query the search query
     * @param userId the user ID
     * @param page the page number
     * @param size the page size
     * @return search response with results
     */
    SearchResponse searchFiles(String query, Long userId, int page, int size);

    /**
     * Search by file content
     *
     * @param query the search query
     * @param userId the user ID
     * @param page the page number
     * @param size the page size
     * @return search response with results
     */
    SearchResponse searchByContent(String query, Long userId, int page, int size);

    /**
     * Search by file type with optional text query
     *
     * @param fileType the file type
     * @param query optional search query
     * @param userId the user ID
     * @param page the page number
     * @param size the page size
     * @return search response with results
     */
    SearchResponse searchByFileType(String fileType, String query, Long userId, int page, int size);

    /**
     * Search by tag
     *
     * @param tag the tag to search for
     * @param userId the user ID
     * @param page the page number
     * @param size the page size
     * @return search response with results
     */
    SearchResponse searchByTag(String tag, Long userId, int page, int size);

    /**
     * Get file search documents by query
     *
     * @param query the search query
     * @param userId the user ID
     * @param page the page number
     * @param size the page size
     * @return list of file search documents
     */
    List<FileSearchDocument> searchFileDocuments(String query, Long userId, int page, int size);

    /**
     * Reindex all files for a user
     *
     * @param userId the user ID
     */
    void reindexUserFiles(Long userId);
}