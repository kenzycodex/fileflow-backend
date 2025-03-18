package com.fileflow.repository.search;

import com.fileflow.model.search.FileSearchDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Elasticsearch operations on files
 */
@Repository
public interface FileSearchRepository extends ElasticsearchRepository<FileSearchDocument, Long> {

    /**
     * Full-text search across multiple fields
     */
    @Query("{\"bool\": {\"must\": [{\"bool\": {\"should\": [{\"match\": {\"filename\": \"?0\"}}, {\"match\": {\"content\": \"?0\"}}, {\"match\": {\"tags\": \"?0\"}}]}}, {\"term\": {\"userId\": \"?1\"}}, {\"term\": {\"isDeleted\": false}}]}}")
    Page<FileSearchDocument> search(String query, Long userId, Pageable pageable);

    /**
     * Search by file type
     */
    Page<FileSearchDocument> findByUserIdAndFileTypeAndIsDeletedFalse(Long userId, String fileType, Pageable pageable);

    /**
     * Search by parent folder
     */
    Page<FileSearchDocument> findByUserIdAndParentFolderIdAndIsDeletedFalse(Long userId, Long parentFolderId, Pageable pageable);

    /**
     * Search by tag
     */
    Page<FileSearchDocument> findByUserIdAndTagsContainingAndIsDeletedFalse(Long userId, String tag, Pageable pageable);

    /**
     * Content-based search
     */
    @Query("{\"bool\": {\"must\": [{\"match\": {\"content\": \"?0\"}}, {\"term\": {\"userId\": \"?1\"}}, {\"term\": {\"isDeleted\": false}}]}}")
    Page<FileSearchDocument> searchContent(String query, Long userId, Pageable pageable);

    /**
     * Delete by user ID
     */
    void deleteByUserId(Long userId);
}