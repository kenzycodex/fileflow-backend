package com.fileflow.service.trash;

import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.common.SearchResponse;
import com.fileflow.dto.response.file.FileResponse;
import com.fileflow.dto.response.folder.FolderResponse;
import com.fileflow.model.File;
import com.fileflow.model.Folder;
import com.fileflow.model.User;
import com.fileflow.repository.FileRepository;
import com.fileflow.repository.FolderRepository;
import com.fileflow.repository.UserRepository;
import com.fileflow.security.UserPrincipal;
import com.fileflow.service.activity.ActivityService;
import com.fileflow.service.quota.QuotaService;
import com.fileflow.service.storage.StorageService;
import com.fileflow.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrashServiceImpl implements TrashService {

    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final ActivityService activityService;
    private final QuotaService quotaService;
    private final StorageService storageService;

    @Override
    public SearchResponse getTrashItems(int page, int size) {
        User currentUser = getCurrentUser();
        validatePageNumberAndSize(page, size);

        // Adjust size to split between files and folders
        int adjustedSize = size / 2;
        if (adjustedSize < 1) adjustedSize = 1;

        // Get deleted files
        Pageable filePageable = PageRequest.of(page, adjustedSize, Sort.Direction.DESC, "deletedAt");
        Page<File> files = fileRepository.findByUserAndIsDeletedTrue(currentUser, filePageable);

        // Get deleted folders
        Pageable folderPageable = PageRequest.of(page, adjustedSize, Sort.Direction.DESC, "deletedAt");
        Page<Folder> folders = folderRepository.findByUserAndIsDeletedTrue(currentUser, folderPageable);

        // Map results
        List<FileResponse> fileResponses = files.getContent().stream()
                .map(this::mapFileToFileResponse)
                .collect(Collectors.toList());

        List<FolderResponse> folderResponses = folders.getContent().stream()
                .map(this::mapFolderToFolderResponse)
                .collect(Collectors.toList());

        // Calculate total elements and pages
        long totalElements = files.getTotalElements() + folders.getTotalElements();
        int totalPages = Math.max(files.getTotalPages(), folders.getTotalPages());

        return SearchResponse.builder()
                .files(fileResponses)
                .folders(folderResponses)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasMore(page < totalPages - 1)
                .build();
    }

    @Override
    @Transactional
    public ApiResponse emptyTrash() {
        User currentUser = getCurrentUser();

        // Get all deleted files
        List<File> deletedFiles = fileRepository.findByUserAndIsDeletedTrue(currentUser);

        // Calculate total size to release
        long totalSize = deletedFiles.stream()
                .mapToLong(File::getFileSize)
                .sum();

        // Delete files
        int filesDeleted = 0;
        for (File file : deletedFiles) {
            try {
                // Only delete from storage if no other files are using the same storage path
                if (!fileRepository.existsByStoragePathAndIsDeletedFalseAndIdNot(
                        file.getStoragePath(), file.getId())) {
                    storageService.delete(file.getStoragePath());
                }

                fileRepository.delete(file);
                filesDeleted++;
            } catch (Exception e) {
                log.error("Error deleting file: {}", file.getId(), e);
            }
        }

        // Delete folders
        List<Folder> deletedFolders = folderRepository.findByUserAndIsDeletedTrue(currentUser);
        int foldersDeleted = 0;

        for (Folder folder : deletedFolders) {
            try {
                folderRepository.delete(folder);
                foldersDeleted++;
            } catch (Exception e) {
                log.error("Error deleting folder: {}", folder.getId(), e);
            }
        }

        // Update user's storage quota
        quotaService.releaseStorage(currentUser.getId(), totalSize);

        // Log activity
        activityService.logActivity(
                currentUser.getId(),
                Constants.ACTIVITY_EMPTY_TRASH,
                null,
                null,
                "Emptied trash"
        );

        Map<String, Object> data = new HashMap<>();
        data.put("filesDeleted", filesDeleted);
        data.put("foldersDeleted", foldersDeleted);
        data.put("totalSizeReleased", totalSize);

        return ApiResponse.builder()
                .success(true)
                .message("Trash emptied successfully")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public ApiResponse restoreAllFromTrash() {
        User currentUser = getCurrentUser();

        // Restore deleted folders first
        List<Folder> deletedFolders = folderRepository.findByUserAndIsDeletedTrue(currentUser);

        for (Folder folder : deletedFolders) {
            try {
                folder.setDeleted(false);
                folder.setDeletedAt(null);
                folderRepository.save(folder);
            } catch (Exception e) {
                log.error("Error restoring folder: {}", folder.getId(), e);
            }
        }

        // Restore deleted files
        List<File> deletedFiles = fileRepository.findByUserAndIsDeletedTrue(currentUser);

        for (File file : deletedFiles) {
            try {
                file.setDeleted(false);
                file.setDeletedAt(null);
                fileRepository.save(file);
            } catch (Exception e) {
                log.error("Error restoring file: {}", file.getId(), e);
            }
        }

        // Log activity
        activityService.logActivity(
                currentUser.getId(),
                Constants.ACTIVITY_RESTORE_ALL,
                null,
                null,
                "Restored all items from trash"
        );

        Map<String, Object> data = new HashMap<>();
        data.put("filesRestored", deletedFiles.size());
        data.put("foldersRestored", deletedFolders.size());

        return ApiResponse.builder()
                .success(true)
                .message("All items restored from trash")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public ApiResponse getTrashInfo() {
        User currentUser = getCurrentUser();

        // Get all deleted files
        List<File> deletedFiles = fileRepository.findByUserAndIsDeletedTrue(currentUser);

        // Calculate total size
        long totalSize = deletedFiles.stream()
                .mapToLong(File::getFileSize)
                .sum();

        // Get all deleted folders
        List<Folder> deletedFolders = folderRepository.findByUserAndIsDeletedTrue(currentUser);

        // Find oldest item in trash to calculate auto-cleanup date
        LocalDateTime oldestDeletedAt = null;

        for (File file : deletedFiles) {
            if (oldestDeletedAt == null || file.getDeletedAt().isBefore(oldestDeletedAt)) {
                oldestDeletedAt = file.getDeletedAt();
            }
        }

        for (Folder folder : deletedFolders) {
            if (oldestDeletedAt == null || folder.getDeletedAt().isBefore(oldestDeletedAt)) {
                oldestDeletedAt = folder.getDeletedAt();
            }
        }

        // Calculate cleanup date (30 days after oldest item was deleted)
        LocalDateTime cleanupDate = oldestDeletedAt != null ?
                oldestDeletedAt.plusDays(Constants.TRASH_RETENTION_DAYS) : null;

        Map<String, Object> data = new HashMap<>();
        data.put("fileCount", deletedFiles.size());
        data.put("folderCount", deletedFolders.size());
        data.put("totalSize", totalSize);
        data.put("oldestItemDate", oldestDeletedAt);
        data.put("cleanupDate", cleanupDate);
        data.put("retentionPeriodDays", Constants.TRASH_RETENTION_DAYS);

        return ApiResponse.builder()
                .success(true)
                .message("Trash information retrieved successfully")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // Helper methods

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void validatePageNumberAndSize(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be less than zero.");
        }

        if (size < 1) {
            throw new IllegalArgumentException("Page size must not be less than one.");
        }

        if (size > 100) {
            throw new IllegalArgumentException("Page size must not be greater than 100.");
        }
    }

    private FileResponse mapFileToFileResponse(File file) {
        return FileResponse.builder()
                .id(file.getId())
                .filename(file.getFilename())
                .originalFilename(file.getOriginalFilename())
                .fileSize(file.getFileSize())
                .fileType(file.getFileType())
                .mimeType(file.getMimeType())
                .parentFolderId(file.getParentFolder() != null ? file.getParentFolder().getId() : null)
                .parentFolderName(file.getParentFolder() != null ? file.getParentFolder().getFolderName() : null)
                .isFavorite(file.isFavorite())
                .createdAt(file.getCreatedAt())
                .updatedAt(file.getUpdatedAt())
                .lastAccessed(file.getLastAccessed())
                .downloadUrl("/api/v1/files/download/" + file.getId())
                .owner(file.getUser().getUsername())
                .ownerId(file.getUser().getId())
                .build();
    }

    private FolderResponse mapFolderToFolderResponse(Folder folder) {
        return FolderResponse.builder()
                .id(folder.getId())
                .folderName(folder.getFolderName())
                .parentFolderId(folder.getParentFolder() != null ? folder.getParentFolder().getId() : null)
                .parentFolderName(folder.getParentFolder() != null ? folder.getParentFolder().getFolderName() : null)
                .createdAt(folder.getCreatedAt())
                .updatedAt(folder.getUpdatedAt())
                .lastAccessed(folder.getLastAccessed())
                .owner(folder.getUser().getUsername())
                .ownerId(folder.getUser().getId())
                .build();
    }
}