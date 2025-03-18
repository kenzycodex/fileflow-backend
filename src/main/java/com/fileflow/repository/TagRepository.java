package com.fileflow.repository;

import com.fileflow.model.Tag;
import com.fileflow.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    /**
     * Find tags by user
     */
    List<Tag> findByUser(User user);

    /**
     * Find tag by user and name
     */
    Optional<Tag> findByUserAndName(User user, String name);

    /**
     * Check if tag exists for user
     */
    boolean existsByUserAndName(User user, String name);

    /**
     * Find popular tags for user (most used)
     */
    @Query("SELECT t FROM Tag t JOIN t.fileTags ft WHERE t.user = :user " +
            "GROUP BY t.id ORDER BY COUNT(ft) DESC")
    List<Tag> findPopularTags(@Param("user") User user);

    /**
     * Find tags for a specific file
     */
    @Query("SELECT t FROM Tag t JOIN t.fileTags ft WHERE ft.file.id = :fileId")
    List<Tag> findTagsByFileId(@Param("fileId") Long fileId);
}