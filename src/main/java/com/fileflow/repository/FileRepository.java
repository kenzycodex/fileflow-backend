package com.fileflow.repository;

import com.fileflow.model.File;
import com.fileflow.model.Folder;
import com.fileflow.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {
    /**
     * Find files by user where not deleted
     */
    List<File> findByUserAndIsDeletedFalse(User user);

    /**
     * Find files by user where not deleted with pagination
     */
    Page<File> findByUserAndIsDeletedFalse(User user, Pageable pageable);

    /**
     * Find files by user and parent folder where not deleted
     */
    List<File> findByUserAndParentFolderAndIsDeletedFalse(User user, Folder parentFolder);

    /**
     * Find files by user and parent folder where not deleted with pagination
     */
    Page<File> findByUserAndParentFolderAndIsDeletedFalse(User user, Folder parentFolder, Pageable pageable);

    /**
     * Find deleted files by user
     */
    List<File> findByUserAndIsDeletedTrue(User user);

    /**
     * Find deleted files by user with pagination
     */
    Page<File> findByUserAndIsDeletedTrue(User user, Pageable pageable);

    /**
     * Find file by ID, user, and not deleted
     */
    Optional<File> findByIdAndUserAndIsDeletedFalse(Long fileId, User user);

    /**
     * Search files by filename
     */
    @Query("SELECT f FROM File f WHERE f.user = :user AND f.isDeleted = false AND lower(f.filename) LIKE lower(concat('%', :keyword, '%'))")
    Page<File> searchByFilename(@Param("user") User user, @Param("keyword") String keyword, Pageable pageable);

    /**
     * Search files by filename and file type
     */
    @Query("SELECT f FROM File f WHERE f.user = :user AND f.isDeleted = false AND f.fileType = :fileType " +
            "AND lower(f.filename) LIKE lower(concat('%', :keyword, '%'))")
    Page<File> searchByFileTypeAndFilename(@Param("user") User user, @Param("fileType") String fileType,
                                           @Param("keyword") String keyword, Pageable pageable);

    /**
     * Search deleted files by filename
     */
    @Query("SELECT f FROM File f WHERE f.user = :user AND f.isDeleted = true AND lower(f.filename) LIKE lower(concat('%', :keyword, '%'))")
    Page<File> searchDeletedByFilename(@Param("user") User user, @Param("keyword") String keyword, Pageable pageable);

    /**
     * Find files by user and file type where not deleted
     */
    Page<File> findByUserAndFileTypeAndIsDeletedFalse(User user, String fileType, Pageable pageable);

    /**
     * Find favorite files
     */
    @Query("SELECT f FROM File f WHERE f.user = :user AND f.isDeleted = false AND f.isFavorite = true")
    Page<File> findFavorites(@Param("user") User user, Pageable pageable);

    /**
     * Find recent files
     */
    @Query("SELECT f FROM File f WHERE f.user = :user AND f.isDeleted = false ORDER BY f.lastAccessed DESC NULLS LAST")
    Page<File> findRecentFiles(@Param("user") User user, Pageable pageable);

    /**
     * Find recent files without pagination
     */
    @Query("SELECT f FROM File f WHERE f.user = :user AND f.isDeleted = false ORDER BY f.lastAccessed DESC NULLS LAST")
    List<File> findRecentFiles(@Param("user") User user);

    @Query("SELECT COUNT(f) FROM File f WHERE f.user = :user AND f.isDeleted = false AND f.createdAt < :date")
    long countByUserAndCreatedAtBeforeAndIsDeletedFalse(@Param("user") User user, @Param("date") LocalDateTime date);

    @Query("SELECT SUM(f.fileSize) FROM File f WHERE f.user = :user AND f.isDeleted = false AND f.createdAt < :date")
    Long sumFileSizeByUserAndCreatedAtBeforeAndIsDeletedFalse(@Param("user") User user, @Param("date") LocalDateTime date);

    @Query("SELECT COUNT(f) FROM File f WHERE f.isDeleted = true")
    long countByIsDeletedTrue();

    @Query("SELECT f FROM File f WHERE f.user = :user AND f.createdAt > :date")
    List<File> findByUserAndCreatedAtAfter(@Param("user") User user, @Param("date") LocalDateTime date);

    @Query("SELECT f FROM File f WHERE f.createdAt BETWEEN :start AND :end")
    List<File> findByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Find by storage path
     */
    boolean existsByStoragePath(String storagePath);

    /**
     * Find files that have been in trash longer than a specified time
     */
    List<File> findByIsDeletedTrueAndDeletedAtBefore(LocalDateTime cutoffDate);

    /**
     * Count files in a folder
     */
    long countByParentFolderAndIsDeletedFalse(Folder folder);

    /**
     * Count files by user
     */
    long countByUserAndIsDeletedFalse(User user);

    /**
     * Count deleted files by user
     */
    long countByUserAndIsDeletedTrue(User user);

    /**
     * Count files by type
     */
    @Query("SELECT COUNT(f) FROM File f WHERE f.user = :user AND f.fileType = :fileType AND f.isDeleted = false")
    long countByUserAndFileType(@Param("user") User user, @Param("fileType") String fileType);

    /**
     * Find files that don't have thumbnails
     */
    @Query("SELECT f FROM File f WHERE f.user = :user AND f.isDeleted = false AND (f.thumbnailPath IS NULL OR f.thumbnailPath = '')")
    List<File> findFilesWithoutThumbnails(@Param("user") User user);

    /**
     * Find files by checksum (for deduplication)
     */
    List<File> findByUserAndChecksum(User user, String checksum);

    @Query("SELECT f FROM File f WHERE f.user = :user AND f.isDeleted = false AND f.fileType = :fileType")
    Page<File> findByFileType(@Param("user") User user, @Param("fileType") String fileType, Pageable pageable);

    @Query("SELECT f FROM File f WHERE f.user = :user AND f.isFavorite = true AND f.isDeleted = false")
    Page<File> findFavoriteFiles(@Param("user") User user, Pageable pageable);

    /**
     * Check if a file with the same storage path exists and is not deleted, excluding the current file
     */
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM File f " +
            "WHERE f.storagePath = :storagePath AND f.isDeleted = false AND f.id <> :id")
    boolean existsByStoragePathAndIsDeletedFalseAndIdNot(
            @Param("storagePath") String storagePath,
            @Param("id") Long id);
}