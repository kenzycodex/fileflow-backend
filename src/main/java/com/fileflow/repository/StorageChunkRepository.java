package com.fileflow.repository;

import com.fileflow.model.StorageChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StorageChunkRepository extends JpaRepository<StorageChunk, Long> {

    /**
     * Find chunks by upload ID and user ID
     */
    List<StorageChunk> findByUploadIdAndUserId(String uploadId, Long userId);

    /**
     * Find chunks by user ID
     */
    List<StorageChunk> findByUserId(Long userId);

    /**
     * Find expired chunks
     */
    List<StorageChunk> findByExpiresAtBefore(LocalDateTime expiryTime);

    /**
     * Count chunks by upload ID
     */
    long countByUploadId(String uploadId);

    /**
     * Delete chunks by upload ID
     */
    void deleteByUploadId(String uploadId);

    /**
     * Find incomplete uploads (where all chunks have not been received)
     */
    @Query("SELECT DISTINCT sc.uploadId FROM StorageChunk sc " +
            "GROUP BY sc.uploadId HAVING COUNT(sc) < MIN(sc.totalChunks)")
    List<String> findIncompleteUploads();

    /**
     * Find chunks that were created before a specific time
     */
    List<StorageChunk> findByCreatedAtBefore(LocalDateTime time);
}