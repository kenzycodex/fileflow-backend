package com.fileflow.repository;

import com.fileflow.model.FileVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileVersionRepository extends JpaRepository<FileVersion, Long> {

    /**
     * Find versions by file ID
     */
    List<FileVersion> findByFileId(Long fileId);

    /**
     * Check if any other versions use the same storage path
     */
    boolean existsByStoragePathAndIdNot(String storagePath, Long id);

    /**
     * Find file IDs with more than N versions
     */
    @Query("SELECT DISTINCT v.file.id FROM FileVersion v " +
            "GROUP BY v.file.id HAVING COUNT(v) > :n")
    List<Long> findFileIdsWithMoreThanNVersions(@Param("n") int n);

    /**
     * Delete versions by file ID
     */
    void deleteByFileId(Long fileId);

    /**
     * Find oldest version for a file
     */
    @Query("SELECT v FROM FileVersion v WHERE v.file.id = :fileId " +
            "ORDER BY v.versionNumber ASC")
    List<FileVersion> findOldestVersions(@Param("fileId") Long fileId);

    /**
     * Find latest version for a file
     */
    @Query("SELECT v FROM FileVersion v WHERE v.file.id = :fileId " +
            "ORDER BY v.versionNumber DESC")
    List<FileVersion> findLatestVersions(@Param("fileId") Long fileId);
}