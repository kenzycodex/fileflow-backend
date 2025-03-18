package com.fileflow.service.search;

import com.fileflow.dto.response.common.SearchResponse;
import com.fileflow.model.File;

/**
 * Service for searching files and folders
 */
public interface SearchService {
    /**
     * Search files and folders by query string
     *
     * @param query the search query
     * @param page page number
     * @param size page size
     * @return search results
     */
    SearchResponse search(String query, int page, int size);

    /**
     * Get recently accessed items
     *
     * @param page page number
     * @param size page size
     * @return recent items
     */
    SearchResponse getRecentItems(int page, int size);

    /**
     * Get favorite items
     *
     * @param page page number
     * @param size page size
     * @return favorite items
     */
    SearchResponse getFavoriteItems(int page, int size);

    /**
     * Search files only
     *
     * @param query the search query
     * @param page page number
     * @param size page size
     * @return file search results
     */
    SearchResponse searchFiles(String query, int page, int size);

    /**
     * Search folders only
     *
     * @param query the search query
     * @param page page number
     * @param size page size
     * @return folder search results
     */
    SearchResponse searchFolders(String query, int page, int size);

    /**
     * Search files by type
     *
     * @param fileType the file type (document, image, etc.)
     * @param query optional search query
     * @param page page number
     * @param size page size
     * @return search results
     */
    SearchResponse searchByFileType(String fileType, String query, int page, int size);

    /**
     * Search items in trash
     *
     * @param query optional search query
     * @param page page number
     * @param size page size
     * @return search results
     */
    SearchResponse searchTrash(String query, int page, int size);

    /**
     * Search file contents (full-text search)
     * Only available when Elasticsearch is enabled
     *
     * @param query the search query
     * @param page page number
     * @param size page size
     * @return search results with content matches
     */
    SearchResponse searchFileContents(String query, int page, int size);

    /**
     * Search files by tag
     *
     * @param tag the tag to search for
     * @param page page number
     * @param size page size
     * @return files with the specified tag
     */
    SearchResponse searchByTag(String tag, int page, int size);

    /**
     * Index a file for search
     * This is a no-op when Elasticsearch is not enabled
     *
     * @param file the file to index
     */
    void indexFile(File file);

    /**
     * Remove a file from the search index
     * This is a no-op when Elasticsearch is not enabled
     *
     * @param fileId the file ID to remove
     */
    void removeFileFromIndex(Long fileId);
}