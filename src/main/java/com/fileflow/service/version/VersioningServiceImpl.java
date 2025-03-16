package com.fileflow.service.version;

import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.file.FileVersionResponse;
import com.fileflow.exception.BadRequestException;
import com.fileflow.exception.ForbiddenException;
import com.fileflow.exception.ResourceNotFoundException;
import com.fileflow.model.File;
import com.fileflow.model.FileVersion;
import com.fileflow.model.User;
import com.fileflow.repository.FileRepository;
import com.fileflow.repository.FileVersionRepository;
import com.fileflow.repository.UserRepository;
import com.fileflow.security.UserPrincipal;
import com.fileflow.service.activity.ActivityService;
import com.fileflow.service.quota.QuotaService;
import com.fileflow.service.storage.StorageService;
import com.fileflow.util.Constants;
import com.fileflow.util.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VersioningServiceImpl implements VersioningService {

    private final FileRepository fileRepository;
    private final FileVersionRepository fileVersionRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final QuotaService quotaService;
    private final ActivityService activityService;

    @Override
    @Transactional
    public FileVersionResponse createVersion(Long fileId, MultipartFile file, String comment) throws IOException {
        User currentUser = getCurrentUser();

        // Find file
        File fileEntity = fileRepository.findByIdAndUserAndIsDeletedFalse(fileId, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("File", "id", fileId));

        // Check file size against quota
        long fileSize = file.getSize();
        if (!quotaService.checkAndReserveQuota(currentUser.getId(), fileSize)) {
            throw new BadRequestException("Insufficient storage quota");
        }

        // Get current version number (if any)
        int nextVersionNumber = 1;
        List<FileVersion> existingVersions = fileVersionRepository.findByFileId(fileId);

        if (!existingVersions.isEmpty()) {
            nextVersionNumber = existingVersions.stream()
                    .mapToInt(FileVersion::getVersionNumber)
                    .max()
                    .orElse(0) + 1;
        }

        // Store current file version before updating it
        String originalStoragePath = fileEntity.getStoragePath();

        // Create version record
        FileVersion version = FileVersion.builder()
                .file(fileEntity)
                .storagePath(originalStoragePath)
                .versionNumber(nextVersionNumber)
                .fileSize(fileEntity.getFileSize())
                .created_at(LocalDateTime.now())
                .created_by(currentUser)
                .comment(comment)
                .build();

        FileVersion savedVersion = fileVersionRepository.save(version);

        // Store new file content
        String uniqueFilename = FileUtils.generateUniqueFilename(file.getOriginalFilename());
        String storageDir = "users/" + currentUser.getId();
        String newStoragePath = storageService.store(file, uniqueFilename, storageDir);

        // Update file record
        fileEntity.setStoragePath(newStoragePath);
        fileEntity.setFileSize(fileSize);
        fileEntity.setUpdatedAt(LocalDateTime.now());
        fileRepository.save(fileEntity);

        // Update user's storage usage
        quotaService.confirmQuotaUsage(currentUser.getId(), fileSize);

        // Log activity
        activityService.logActivity(
                currentUser.getId(),
                Constants.ACTIVITY_CREATE_VERSION,
                Constants.ITEM_TYPE_FILE,
                fileId,
                "Created new version of file: " + fileEntity.getFilename()
        );

        return mapVersionToVersionResponse(savedVersion);
    }

    @Override
    public List<FileVersionResponse> getVersions(Long fileId) {
        User currentUser = getCurrentUser();

        // Find file
        File fileEntity = fileRepository.findByIdAndUserAndIsDeletedFalse(fileId, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("File", "id", fileId));

        // Get versions
        List<FileVersion> versions = fileVersionRepository.findByFileId(fileId);

        // Sort by version number (latest first)
        versions.sort(Comparator.comparing(FileVersion::getVersionNumber).reversed());

        return versions.stream()
                .map(this::mapVersionToVersionResponse)
                .collect(Collectors.toList());
    }

    @Override
    public FileVersionResponse getVersion(Long versionId) {
        User currentUser = getCurrentUser();

        // Find version
        FileVersion version = fileVersionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Version", "id", versionId));

        // Check if user owns the file
        if (!version.getFile().getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You don't have permission to access this version");
        }

        return mapVersionToVersionResponse(version);
    }

    @Override
    @Transactional
    public ApiResponse restoreVersion(Long fileId, Long versionId) {
        User currentUser = getCurrentUser();

        // Find file
        File fileEntity = fileRepository.findByIdAndUserAndIsDeletedFalse(fileId, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("File", "id", fileId));

        // Find version
        FileVersion version = fileVersionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Version", "id", versionId));

        // Check if version belongs to file
        if (!version.getFile().getId().equals(fileId)) {
            throw new BadRequestException("Version does not belong to specified file");
        }

        // Create new version record for current state before restoring
        FileVersion newVersion = FileVersion.builder()
                .file(fileEntity)
                .storagePath(fileEntity.getStoragePath())
                .versionNumber(getNextVersionNumber(fileId))
                .fileSize(fileEntity.getFileSize())
                .created_at(LocalDateTime.now())
                .created_by(currentUser)
                .comment("Automatic version before restoring to version " + version.getVersionNumber())
                .build();

        fileVersionRepository.save(newVersion);

        // Update file record to point to version's storage path
        fileEntity.setStoragePath(version.getStoragePath());
        fileEntity.setFileSize(version.getFileSize());
        fileEntity.setUpdatedAt(LocalDateTime.now());
        fileRepository.save(fileEntity);

        // Log activity
        activityService.logActivity(
                currentUser.getId(),
                Constants.ACTIVITY_RESTORE_VERSION,
                Constants.ITEM_TYPE_FILE,
                fileId,
                "Restored file to version " + version.getVersionNumber() + ": " + fileEntity.getFilename()
        );

        return ApiResponse.builder()
                .success(true)
                .message("File restored to version " + version.getVersionNumber())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public ApiResponse deleteVersion(Long versionId) {
        User currentUser = getCurrentUser();

        // Find version
        FileVersion version = fileVersionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Version", "id", versionId));

        // Check if user owns the file
        if (!version.getFile().getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You don't have permission to delete this version");
        }

        // Check if any other versions or the current file use the same storage path
        boolean isStoragePathUsed = fileVersionRepository.existsByStoragePathAndIdNot(
                version.getStoragePath(), versionId);

        isStoragePathUsed = isStoragePathUsed || fileRepository.existsByStoragePath(version.getStoragePath());

        // Delete version
        fileVersionRepository.delete(version);

        // Release quota if storage path is not used elsewhere
        if (!isStoragePathUsed) {
            quotaService.releaseStorage(currentUser.getId(), version.getFileSize());
            storageService.delete(version.getStoragePath());
        }

        // Log activity
        activityService.logActivity(
                currentUser.getId(),
                Constants.ACTIVITY_DELETE_VERSION,
                Constants.ITEM_TYPE_FILE,
                version.getFile().getId(),
                "Deleted version " + version.getVersionNumber() + " of file: " + version.getFile().getFilename()
        );

        return ApiResponse.builder()
                .success(true)
                .message("Version deleted successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public int cleanupOldVersions(int maxVersionsPerFile) {
        // Get all files with more than maxVersionsPerFile versions
        List<Long> fileIdsWithTooManyVersions = fileVersionRepository.findFileIdsWithMoreThanNVersions(
                maxVersionsPerFile);

        int deletedCount = 0;

        for (Long fileId : fileIdsWithTooManyVersions) {
            // Get all versions for file
            List<FileVersion> versions = fileVersionRepository.findByFileId(fileId);

            // Sort by version number (oldest first)
            versions.sort(Comparator.comparing(FileVersion::getVersionNumber));

            // Calculate how many to delete
            int versionsToDeleteCount = versions.size() - maxVersionsPerFile;

            if (versionsToDeleteCount > 0) {
                // Delete oldest versions
                List<FileVersion> versionsToDelete = versions.subList(0, versionsToDeleteCount);

                for (FileVersion version : versionsToDelete) {
                    try {
                        // Check if storage path is used elsewhere
                        boolean isStoragePathUsed = fileVersionRepository.existsByStoragePathAndIdNot(
                                version.getStoragePath(), version.getId());

                        isStoragePathUsed = isStoragePathUsed || fileRepository.existsByStoragePath(version.getStoragePath());

                        // Delete version
                        fileVersionRepository.delete(version);

                        // Delete from storage if not used elsewhere
                        if (!isStoragePathUsed) {
                            // Release quota
                            quotaService.releaseStorage(version.getFile().getUser().getId(), version.getFileSize());
                            storageService.delete(version.getStoragePath());
                        }

                        deletedCount++;
                    } catch (Exception e) {
                        log.error("Error deleting version {}", version.getId(), e);
                    }
                }
            }
        }

        log.info("Cleaned up {} old versions", deletedCount);
        return deletedCount;
    }

    // Helper methods

    private int getNextVersionNumber(Long fileId) {
        List<FileVersion> existingVersions = fileVersionRepository.findByFileId(fileId);

        if (existingVersions.isEmpty()) {
            return 1;
        }

        return existingVersions.stream()
                .mapToInt(FileVersion::getVersionNumber)
                .max()
                .orElse(0) + 1;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userPrincipal.getId()));
    }

    private FileVersionResponse mapVersionToVersionResponse(FileVersion version) {
        // Generate download URL
        String downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/files/versions/")
                .path(version.getId().toString())
                .path("/download")
                .toUriString();

        return FileVersionResponse.builder()
                .id(version.getId())
                .fileId(version.getFile().getId())
                .versionNumber(version.getVersionNumber())
                .fileSize(version.getFileSize())
                .createdAt(version.getCreated_at())
                .createdBy(version.getCreated_by().getUsername())
                .createdById(version.getCreated_by().getId())
                .comment(version.getComment())
                .downloadUrl(downloadUrl)
                .build();
    }
}