package com.fileflow.repository;

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
public interface FolderRepository extends JpaRepository<Folder, Long> {
    List<Folder> findByUserAndIsDeletedFalse(User user);

    Page<Folder> findByUserAndIsDeletedFalse(User user, Pageable pageable);

    List<Folder> findByUserAndParentFolderAndIsDeletedFalse(User user, Folder parentFolder);

    Page<Folder> findByUserAndParentFolderAndIsDeletedFalse(User user, Folder parentFolder, Pageable pageable);

    List<Folder> findByUserAndIsDeletedTrue(User user);

    Page<Folder> findByUserAndIsDeletedTrue(User user, Pageable pageable);

    Optional<Folder> findByIdAndUserAndIsDeletedFalse(Long folderId, User user);

    @Query("SELECT f FROM Folder f WHERE f.user = :user AND f.isDeleted = false AND lower(f.folderName) LIKE lower(concat('%', :keyword, '%'))")
    Page<Folder> searchByFolderName(@Param("user") User user, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT f FROM Folder f WHERE f.user = :user AND f.isDeleted = false ORDER BY f.lastAccessed DESC NULLS LAST")
    Page<Folder> findRecentFolders(@Param("user") User user, Pageable pageable);

    @Query("SELECT f FROM Folder f WHERE f.user = :user AND f.isDeleted = false ORDER BY f.lastAccessed DESC NULLS LAST")
    List<Folder> findRecentFolders(@Param("user") User user);

    @Query("SELECT f FROM Folder f WHERE f.user = :user AND f.isFavorite = true AND f.isDeleted = false")
    Page<Folder> findFavoriteFolders(@Param("user") User user, Pageable pageable);

    @Query("SELECT f FROM Folder f WHERE f.user = :user AND f.isFavorite = true AND f.isDeleted = false")
    List<Folder> findFavoriteFolders(@Param("user") User user);

    // Add this method to search deleted folders by name
    @Query("SELECT f FROM Folder f WHERE f.user = :user AND f.isDeleted = true AND lower(f.folderName) LIKE lower(concat('%', :keyword, '%'))")
    Page<Folder> searchDeletedByFolderName(@Param("user") User user, @Param("keyword") String keyword, Pageable pageable);
}