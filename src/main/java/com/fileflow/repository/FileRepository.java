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

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {
    List<File> findByUserAndIsDeletedFalse(User user);

    Page<File> findByUserAndIsDeletedFalse(User user, Pageable pageable);

    List<File> findByUserAndParentFolderAndIsDeletedFalse(User user, Folder parentFolder);

    Page<File> findByUserAndParentFolderAndIsDeletedFalse(User user, Folder parentFolder, Pageable pageable);

    List<File> findByUserAndIsDeletedTrue(User user);

    Page<File> findByUserAndIsDeletedTrue(User user, Pageable pageable);

    Optional<File> findByIdAndUserAndIsDeletedFalse(Long fileId, User user);

    @Query("SELECT f FROM File f WHERE f.user = :user AND f.isDeleted = false AND lower(f.filename) LIKE lower(concat('%', :keyword, '%'))")
    Page<File> searchByFilename(@Param("user") User user, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT f FROM File f WHERE f.user = :user AND f.isDeleted = false AND f.isFavorite = true")
    Page<File> findFavorites(@Param("user") User user, Pageable pageable);

    @Query("SELECT f FROM File f WHERE f.user = :user AND f.isDeleted = false ORDER BY f.lastAccessed DESC NULLS LAST")
    Page<File> findRecentFiles(@Param("user") User user, Pageable pageable);
}