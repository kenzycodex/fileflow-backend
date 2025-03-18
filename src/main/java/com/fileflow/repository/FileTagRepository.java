package com.fileflow.repository;

import com.fileflow.model.File;
import com.fileflow.model.FileTag;
import com.fileflow.model.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for file tag operations
 */
@Repository
public interface FileTagRepository extends JpaRepository<FileTag, Long> {

    /**
     * Find file tag by file and tag
     */
    Optional<FileTag> findByFileAndTag(File file, Tag tag);

    /**
     * Find tags for a file
     */
    List<FileTag> findByFile(File file);

    /**
     * Find file tags by tag
     */
    List<FileTag> findByTag(Tag tag);

    /**
     * Find files by tag name and user ID
     */
    @Query("SELECT ft.file FROM FileTag ft " +
            "WHERE ft.tag.name = :tagName " +
            "AND ft.file.user.id = :userId " +
            "AND ft.file.isDeleted = false " +
            "ORDER BY ft.file.lastAccessed DESC")
    List<File> findFilesByTagNameAndUserId(@Param("tagName") String tagName,
                                           @Param("userId") Long userId,
                                           Pageable pageable);

    /**
     * Count tags for a file
     */
    long countByFile(File file);

    /**
     * Delete all file tags for a file
     */
    void deleteByFile(File file);

    /**
     * Delete all file tags for a tag
     */
    void deleteByTag(Tag tag);

    /**
     * Delete by file and tag
     */
    void deleteByFileAndTag(File file, Tag tag);

    /**
     * Find popular tags for a user
     */
    @Query("SELECT ft.tag.name, COUNT(ft) as count FROM FileTag ft " +
            "WHERE ft.file.user.id = :userId " +
            "AND ft.file.isDeleted = false " +
            "GROUP BY ft.tag.name " +
            "ORDER BY count DESC")
    List<Object[]> findPopularTagsByUserId(@Param("userId") Long userId, Pageable pageable);
}