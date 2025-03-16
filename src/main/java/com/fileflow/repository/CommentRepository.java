package com.fileflow.repository;

import com.fileflow.model.Comment;
import com.fileflow.model.File;
import com.fileflow.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByFile(File file);

    Page<Comment> findByFile(File file, Pageable pageable);

    List<Comment> findByFileAndParentCommentIsNull(File file);

    Page<Comment> findByFileAndParentCommentIsNull(File file, Pageable pageable);

    List<Comment> findByParentComment(Comment parentComment);

    List<Comment> findByUser(User user);

    Page<Comment> findByUser(User user, Pageable pageable);
}