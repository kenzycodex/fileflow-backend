package com.fileflow.repository;

import com.fileflow.model.Comment;
import com.fileflow.model.File;
import com.fileflow.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * Find comments by file
     */
    List<Comment> findByFileAndIsDeletedFalse(File file);

    /**
     * Find comments by file with pagination
     */
    Page<Comment> findByFileAndIsDeletedFalse(File file, Pageable pageable);

    /**
     * Find root comments (no parent) by file
     */
    List<Comment> findByFileAndParentCommentIsNullAndIsDeletedFalse(File file);

    /**
     * Find replies to a comment
     */
    List<Comment> findByParentCommentAndIsDeletedFalse(Comment parentComment);

    /**
     * Find comments by user
     */
    List<Comment> findByUserAndIsDeletedFalse(User user);

    /**
     * Find recent comments by user
     */
    @Query("SELECT c FROM Comment c WHERE c.user = :user AND c.isDeleted = false " +
            "ORDER BY c.createdAt DESC")
    Page<Comment> findRecentCommentsByUser(@Param("user") User user, Pageable pageable);

    /**
     * Count comments for a file
     */
    long countByFileAndIsDeletedFalse(File file);

    List<Comment> findByFile(File file);

    Page<Comment> findByFile(File file, Pageable pageable);

    List<Comment> findByFileAndParentCommentIsNull(File file);

    Page<Comment> findByFileAndParentCommentIsNull(File file, Pageable pageable);

    List<Comment> findByParentComment(Comment parentComment);

    List<Comment> findByUser(User user);

    Page<Comment> findByUser(User user, Pageable pageable);

    /**
     * Search comments by content
     */
    @Query("SELECT c FROM Comment c WHERE c.file = :file AND c.isDeleted = false " +
            "AND LOWER(c.commentText) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Comment> searchByContent(@Param("file") File file, @Param("keyword") String keyword);
}