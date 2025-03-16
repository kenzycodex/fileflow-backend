package com.fileflow.service.folder;

import com.fileflow.dto.request.folder.FolderCreateRequest;
import com.fileflow.dto.request.folder.FolderUpdateRequest;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.file.FileResponse;
import com.fileflow.dto.response.folder.FolderContentsResponse;
import com.fileflow.dto.response.folder.FolderResponse;
import com.fileflow.exception.BadRequestException;
import com.fileflow.exception.ForbiddenException;
import com.fileflow.exception.ResourceNotFoundException;
import com.fileflow.model.File;
import com.fileflow.model.Folder;
import com.fileflow.model.User;
import com.fileflow.repository.FileRepository;
import com.fileflow.repository.FolderRepository;
import com.fileflow.repository.UserRepository;
import com.fileflow.security.UserPrincipal;
import com.fileflow.service.activity.ActivityService;
import com.fileflow.service.file.FileService;
import com.fileflow.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FolderServiceImpl implements FolderService {

    private final FolderRepository folderRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final ActivityService activityService;

    @Override
    @Transactional
    public FolderResponse createFolder(FolderCreateRequest folderCreateRequest) {
        User currentUser = getCurrentUser();

        // Check if parent folder exists and belongs to the user
        Folder parentFolder = null;
        if (folderCreateRequest.getParentFolderId() != null) {
            parentFolder = folderRepository.findByIdAndUserAndIsDeletedFalse(
                            folderCreateRequest.getParentFolderId(), currentUser)
                    .orElseThrow(() -> new ResourceNotFoundException("Folder", "id",
                            folderCreateRequest.getParentFolderId()));
        }

        // Check if folder with same name already exists in the same parent folder
        boolean folderExists = folderRepository.findByUserAndParentFolderAndIsDeletedFalse(currentUser, parentFolder)
                .stream()
                .anyMatch(folder -> folder.getFolderName().equals(folderCreateRequest.getFolderName()));

        if (folderExists) {
            throw new BadRequestException("Folder with name '" + folderCreateRequest.getFolderName() +
                    "' already exists in this location");
        }

        // Create new folder
        Folder folder = Folder.builder()
                .folderName(folderCreateRequest.getFolderName())
                .user(currentUser)
                .parentFolder(parentFolder)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .lastAccessed(LocalDateTime.now())
                .build();

        Folder savedFolder = folderRepository.save(folder);

        // Log activity
        activityService.logActivity(
                currentUser.getId(),
                Constants.ACTIVITY_CREATE_FOLDER,
                Constants.ITEM_TYPE_FOLDER,
                savedFolder.getId(),
                "Created folder: " + savedFolder.getFolderName()
        );

        return mapFolderToFolderResponse(savedFolder);
    }

    @Override
    public FolderResponse getFolder(Long folderId) {
        User currentUser = getCurrentUser();

        Folder folder = folderRepository.findByIdAndUserAndIsDeletedFalse(folderId, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", folderId));

        // Update last accessed time
        folder.setLastAccessed(LocalDateTime.now());
        folderRepository.save(folder);

        return mapFolderToFolderResponse(folder);
    }

    @Override
    public FolderContentsResponse getFolderContents(Long folderId) {
        User currentUser = getCurrentUser();

        // Special case for root folder (null folderId)
        List<Folder> folders;
        List<File> files;

        if (folderId == null) {
            folders = folderRepository.findByUserAndParentFolderAndIsDeletedFalse(currentUser, null);
            files = fileRepository.findByUserAndParentFolderAndIsDeletedFalse(currentUser, null);
        } else {
            Folder folder = folderRepository.findByIdAndUserAndIsDeletedFalse(folderId, currentUser)
                    .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", folderId));

            // Update last accessed time
            folder.setLastAccessed(LocalDateTime.now());
            folderRepository.save(folder);

            folders = folderRepository.findByUserAndParentFolderAndIsDeletedFalse(currentUser, folder);
            files = fileRepository.findByUserAndParentFolderAndIsDeletedFalse(currentUser, folder);
        }

        List<FolderResponse> folderResponses = folders.stream()
                .map(this::mapFolderToFolderResponse)
                .collect(Collectors.toList());

        List<FileResponse> fileResponses = files.stream()
                .map(this::mapFileToFileResponse)
                .collect(Collectors.toList());

        return FolderContentsResponse.builder()
                .folderId(folderId)
                .folders(folderResponses)
                .files(fileResponses)
                .build();
    }

    @Override
    @Transactional
    public FolderResponse updateFolder(Long folderId, FolderUpdateRequest folderUpdateRequest) {
        User currentUser = getCurrentUser();

        Folder folder = folderRepository.findByIdAndUserAndIsDeletedFalse(folderId, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", folderId));

        // Check if we're trying to update the folder name
        if (folderUpdateRequest.getFolderName() != null &&
                !folderUpdateRequest.getFolderName().equals(folder.getFolderName())) {

            // Check if folder with same name already exists in the same parent folder
            boolean folderExists = folderRepository.findByUserAndParentFolderAndIsDeletedFalse(
                            currentUser, folder.getParentFolder())
                    .stream()
                    .filter(f -> !f.getId().equals(folderId)) // Exclude the current folder
                    .anyMatch(f -> f.getFolderName().equals(folderUpdateRequest.getFolderName()));

            if (folderExists) {
                throw new BadRequestException("Folder with name '" + folderUpdateRequest.getFolderName() +
                        "' already exists in this location");
            }

            // Update folder name
            folder.setFolderName(folderUpdateRequest.getFolderName());
        }

        // Update parent folder if requested
        if (folderUpdateRequest.getParentFolderId() != null &&
                !folderUpdateRequest.getParentFolderId().equals(
                        folder.getParentFolder() != null ? folder.getParentFolder().getId() : null)) {

            // Prevent moving folder to itself or its children
            if (folderUpdateRequest.getParentFolderId().equals(folderId)) {
                throw new BadRequestException("Cannot move a folder into itself");
            }

            // Check if target parent folder exists and belongs to the user
            Folder newParentFolder = null;
            if (folderUpdateRequest.getParentFolderId() > 0) {
                newParentFolder = folderRepository.findByIdAndUserAndIsDeletedFalse(
                                folderUpdateRequest.getParentFolderId(), currentUser)
                        .orElseThrow(() -> new ResourceNotFoundException("Folder", "id",
                                folderUpdateRequest.getParentFolderId()));

                // Check for circular reference (can't move a folder to its descendant)
                if (isDescendant(folder, newParentFolder)) {
                    throw new BadRequestException("Cannot move a folder to its descendant");
                }
            }

            folder.setParentFolder(newParentFolder);
        }

        folder.setUpdatedAt(LocalDateTime.now());
        Folder updatedFolder = folderRepository.save(folder);

        // Log activity
        activityService.logActivity(
                currentUser.getId(),
                Constants.ACTIVITY_UPDATE_FOLDER,
                Constants.ITEM_TYPE_FOLDER,
                updatedFolder.getId(),
                "Updated folder: " + updatedFolder.getFolderName()
        );

        return mapFolderToFolderResponse(updatedFolder);
    }

    @Override
    @Transactional
    public ApiResponse deleteFolder(Long folderId) {
        User currentUser = getCurrentUser();

        Folder folder = folderRepository.findByIdAndUserAndIsDeletedFalse(folderId, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", folderId));

        // Mark folder as deleted
        folder.setDeleted(true);
        folder.setDeletedAt(LocalDateTime.now());
        folderRepository.save(folder);

        // Recursively mark all subfolders and files as deleted
        markFolderContentsAsDeleted(folder);

        // Log activity
        activityService.logActivity(
                currentUser.getId(),
                Constants.ACTIVITY_DELETE,
                Constants.ITEM_TYPE_FOLDER,
                folderId,
                "Moved folder to trash: " + folder.getFolderName()
        );

        return ApiResponse.builder()
                .success(true)
                .message("Folder moved to trash")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public ApiResponse restoreFolder(Long folderId) {
        User currentUser = getCurrentUser();

        // Find the deleted folder
        Folder folder = folderRepository.findById(folderId)
                .filter(f -> f.getUser().getId().equals(currentUser.getId()) && f.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", folderId));

        // Check if parent folder is deleted
        if (folder.getParentFolder() != null && folder.getParentFolder().isDeleted()) {
            // Restore to root if parent is deleted
            folder.setParentFolder(null);
        }

        // Restore folder
        folder.setDeleted(false);
        folder.setDeletedAt(null);
        folderRepository.save(folder);

        // Recursively restore subfolders and files
        restoreFolderContents(folder);

        // Log activity
        activityService.logActivity(
                currentUser.getId(),
                Constants.ACTIVITY_RESTORE,
                Constants.ITEM_TYPE_FOLDER,
                folderId,
                "Restored folder from trash: " + folder.getFolderName()
        );

        return ApiResponse.builder()
                .success(true)
                .message("Folder restored from trash")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public ApiResponse permanentDeleteFolder(Long folderId) {
        User currentUser = getCurrentUser();

        // Find the folder (deleted or not)
        Folder folder = folderRepository.findById(folderId)
                .filter(f -> f.getUser().getId().equals(currentUser.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", folderId));

        // Delete files first (won't delete from storage yet - that's handled by a scheduled task)
        permanentDeleteFolderContents(folder);

        // Delete the folder
        folderRepository.delete(folder);

        // Log activity
        activityService.logActivity(
                currentUser.getId(),
                Constants.ACTIVITY_PERMANENT_DELETE,
                Constants.ITEM_TYPE_FOLDER,
                folderId,
                "Permanently deleted folder: " + folder.getFolderName()
        );

        return ApiResponse.builder()
                .success(true)
                .message("Folder permanently deleted")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public FolderResponse moveFolder(Long folderId, Long destinationFolderId) {
        User currentUser = getCurrentUser();

        Folder folder = folderRepository.findByIdAndUserAndIsDeletedFalse(folderId, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", folderId));

        // Determine destination folder (null for root)
        Folder destinationFolder = null;
        if (destinationFolderId != null && destinationFolderId > 0) {
            destinationFolder = folderRepository.findByIdAndUserAndIsDeletedFalse(destinationFolderId, currentUser)
                    .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", destinationFolderId));

            // Prevent moving folder to itself or its children
            if (folderId.equals(destinationFolderId) || isDescendant(folder, destinationFolder)) {
                throw new BadRequestException("Cannot move a folder into itself or its descendants");
            }
        }

        // Check if folder with same name already exists in destination
        boolean folderExists = folderRepository.findByUserAndParentFolderAndIsDeletedFalse(
                        currentUser, destinationFolder)
                .stream()
                .anyMatch(f -> f.getFolderName().equals(folder.getFolderName()));

        if (folderExists) {
            throw new BadRequestException("Folder with name '" + folder.getFolderName() +
                    "' already exists in the destination folder");
        }

        // Move folder
        folder.setParentFolder(destinationFolder);
        folder.setUpdatedAt(LocalDateTime.now());
        Folder movedFolder = folderRepository.save(folder);

        // Log activity
        activityService.logActivity(
                currentUser.getId(),
                Constants.ACTIVITY_MOVE,
                Constants.ITEM_TYPE_FOLDER,
                folderId,
                "Moved folder: " + folder.getFolderName()
        );

        return mapFolderToFolderResponse(movedFolder);
    }

    @Override
    @Transactional
    public FolderResponse copyFolder(Long folderId, Long destinationFolderId) {
        User currentUser = getCurrentUser();

        Folder sourceFolder = folderRepository.findByIdAndUserAndIsDeletedFalse(folderId, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", folderId));

        // Determine destination folder (null for root)
        Folder destinationFolder = null;
        if (destinationFolderId != null && destinationFolderId > 0) {
            destinationFolder = folderRepository.findByIdAndUserAndIsDeletedFalse(destinationFolderId, currentUser)
                    .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", destinationFolderId));

            // Prevent copying folder to itself or its children
            if (folderId.equals(destinationFolderId) || isDescendant(sourceFolder, destinationFolder)) {
                throw new BadRequestException("Cannot copy a folder into itself or its descendants");
            }
        }

        // Determine new folder name (handle duplicates)
        String newFolderName = sourceFolder.getFolderName();
        boolean nameExists = folderRepository.findByUserAndParentFolderAndIsDeletedFalse(
                        currentUser, destinationFolder)
                .stream()
                .anyMatch(f -> f.getFolderName().equals(newFolderName));

        if (nameExists) {
            newFolderName = generateCopyName(newFolderName);
        }

        // Create new folder
        Folder newFolder = Folder.builder()
                .folderName(newFolderName)
                .user(currentUser)
                .parentFolder(destinationFolder)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .lastAccessed(LocalDateTime.now())
                .build();

        Folder savedFolder = folderRepository.save(newFolder);

        // Recursively copy contents
        copyFolderContents(sourceFolder, savedFolder);

        // Log activity
        activityService.logActivity(
                currentUser.getId(),
                Constants.ACTIVITY_COPY,
                Constants.ITEM_TYPE_FOLDER,
                savedFolder.getId(),
                "Copied folder: " + sourceFolder.getFolderName()
        );

        return mapFolderToFolderResponse(savedFolder);
    }

    @Override
    public List<FolderResponse> getRootFolders() {
        User currentUser = getCurrentUser();

        List<Folder> rootFolders = folderRepository.findByUserAndParentFolderAndIsDeletedFalse(currentUser, null);

        return rootFolders.stream()
                .map(this::mapFolderToFolderResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<FolderResponse> getRecentFolders(int limit) {
        User currentUser = getCurrentUser();

        List<Folder> recentFolders = folderRepository.findRecentFolders(currentUser)
                .stream()
                .limit(limit)
                .collect(Collectors.toList());

        return recentFolders.stream()
                .map(this::mapFolderToFolderResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void createUserRootFolder(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Create "My Files" folder
        Folder rootFolder = Folder.builder()
                .folderName("My Files")
                .user(user)
                .parentFolder(null)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .lastAccessed(LocalDateTime.now())
                .build();

        folderRepository.save(rootFolder);

        log.info("Created root folder for user: {}", user.getUsername());
    }

    @Override
    public List<FolderResponse> getFolderPath(Long folderId) {
        if (folderId == null) {
            return Collections.emptyList();
        }

        User currentUser = getCurrentUser();

        Folder folder = folderRepository.findByIdAndUserAndIsDeletedFalse(folderId, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", folderId));

        List<FolderResponse> path = new ArrayList<>();
        buildFolderPath(folder, path);

        // Reverse to get root -> leaf order
        Collections.reverse(path);

        return path;
    }

    // Helper methods

    private void buildFolderPath(Folder folder, List<FolderResponse> path) {
        if (folder == null) {
            return;
        }

        path.add(mapFolderToFolderResponse(folder));

        if (folder.getParentFolder() != null) {
            buildFolderPath(folder.getParentFolder(), path);
        }
    }

    private boolean isDescendant(Folder ancestor, Folder possibleDescendant) {
        Folder current = possibleDescendant;

        while (current != null) {
            if (current.getId().equals(ancestor.getId())) {
                return true;
            }
            current = current.getParentFolder();
        }

        return false;
    }

    private void markFolderContentsAsDeleted(Folder folder) {
        LocalDateTime now = LocalDateTime.now();

        // Mark subfolders as deleted
        List<Folder> subfolders = folderRepository.findByUserAndParentFolderAndIsDeletedFalse(
                folder.getUser(), folder);

        for (Folder subfolder : subfolders) {
            subfolder.setDeleted(true);
            subfolder.setDeletedAt(now);
            folderRepository.save(subfolder);

            // Recursively mark contents of each subfolder
            markFolderContentsAsDeleted(subfolder);
        }

        // Mark files as deleted
        List<File> files = fileRepository.findByUserAndParentFolderAndIsDeletedFalse(
                folder.getUser(), folder);

        for (File file : files) {
            file.setDeleted(true);
            file.setDeletedAt(now);
            fileRepository.save(file);
        }
    }

    private void restoreFolderContents(Folder folder) {
        // Find all direct subfolders that are deleted
        List<Folder> deletedSubfolders = folderRepository.findAll().stream()
                .filter(f -> f.getParentFolder() != null &&
                        f.getParentFolder().getId().equals(folder.getId()) &&
                        f.isDeleted() &&
                        f.getUser().getId().equals(folder.getUser().getId()))
                .collect(Collectors.toList());

        // Restore subfolders
        for (Folder subfolder : deletedSubfolders) {
            subfolder.setDeleted(false);
            subfolder.setDeletedAt(null);
            folderRepository.save(subfolder);

            // Recursively restore contents
            restoreFolderContents(subfolder);
        }

        // Find all direct files that are deleted
        List<File> deletedFiles = fileRepository.findAll().stream()
                .filter(f -> f.getParentFolder() != null &&
                        f.getParentFolder().getId().equals(folder.getId()) &&
                        f.isDeleted() &&
                        f.getUser().getId().equals(folder.getUser().getId()))
                .collect(Collectors.toList());

        // Restore files
        for (File file : deletedFiles) {
            file.setDeleted(false);
            file.setDeletedAt(null);
            fileRepository.save(file);
        }
    }

    private void permanentDeleteFolderContents(Folder folder) {
        // Delete subfolders first (recursively)
        List<Folder> subfolders = folderRepository.findAll().stream()
                .filter(f -> f.getParentFolder() != null &&
                        f.getParentFolder().getId().equals(folder.getId()) &&
                        f.getUser().getId().equals(folder.getUser().getId()))
                .collect(Collectors.toList());

        for (Folder subfolder : subfolders) {
            permanentDeleteFolderContents(subfolder);
            folderRepository.delete(subfolder);
        }

        // Delete files
        List<File> files = fileRepository.findAll().stream()
                .filter(f -> f.getParentFolder() != null &&
                        f.getParentFolder().getId().equals(folder.getId()) &&
                        f.getUser().getId().equals(folder.getUser().getId()))
                .collect(Collectors.toList());

        for (File file : files) {
            // Remove file from repository (actual storage cleaning is done by a scheduled task)
            fileRepository.delete(file);
        }
    }

    private void copyFolderContents(Folder sourceFolder, Folder targetFolder) {
        User currentUser = targetFolder.getUser();

        // Copy files
        List<File> files = fileRepository.findByUserAndParentFolderAndIsDeletedFalse(
                currentUser, sourceFolder);

        for (File file : files) {
            // Determine if name needs to be modified
            String newFilename = file.getFilename();
            boolean nameExists = fileRepository.findByUserAndParentFolderAndIsDeletedFalse(
                            currentUser, targetFolder)
                    .stream()
                    .anyMatch(f -> f.getFilename().equals(newFilename));

            if (nameExists) {
                newFilename = generateCopyName(newFilename);
            }

            // Create new file record (pointing to same storage object)
            File newFile = File.builder()
                    .filename(newFilename)
                    .originalFilename(file.getOriginalFilename())
                    .storagePath(file.getStoragePath())
                    .fileSize(file.getFileSize())
                    .fileType(file.getFileType())
                    .mimeType(file.getMimeType())
                    .user(currentUser)
                    .parentFolder(targetFolder)
                    .isFavorite(false)
                    .isDeleted(false)
                    .createdAt(LocalDateTime.now())
                    .lastAccessed(LocalDateTime.now())
                    .checksum(file.getChecksum())
                    .build();

            fileRepository.save(newFile);
        }

        // Copy subfolders recursively
        List<Folder> subfolders = folderRepository.findByUserAndParentFolderAndIsDeletedFalse(
                currentUser, sourceFolder);

        for (Folder subfolder : subfolders) {
            // Determine if name needs to be modified
            String newFolderName = subfolder.getFolderName();
            boolean nameExists = folderRepository.findByUserAndParentFolderAndIsDeletedFalse(
                            currentUser, targetFolder)
                    .stream()
                    .anyMatch(f -> f.getFolderName().equals(newFolderName));

            if (nameExists) {
                newFolderName = generateCopyName(newFolderName);
            }

            // Create new subfolder
            Folder newSubfolder = Folder.builder()
                    .folderName(newFolderName)
                    .user(currentUser)
                    .parentFolder(targetFolder)
                    .isDeleted(false)
                    .createdAt(LocalDateTime.now())
                    .lastAccessed(LocalDateTime.now())
                    .build();

            Folder savedSubfolder = folderRepository.save(newSubfolder);

            // Recursively copy contents
            copyFolderContents(subfolder, savedSubfolder);
        }
    }

    private String generateCopyName(String originalName) {
        // Check if name already has copy pattern
        if (originalName.matches(".*\\s+\\(Copy\\s+\\d+\\)$")) {
            // Increment copy number
            int copyNumber = Integer.parseInt(
                    originalName.substring(
                            originalName.lastIndexOf("Copy") + 5,
                            originalName.length() - 1
                    )
            );

            return originalName.substring(0, originalName.lastIndexOf("Copy") + 5) +
                    (copyNumber + 1) + ")";
        } else if (originalName.matches(".*\\s+\\(Copy\\)$")) {
            // Add copy number
            return originalName.substring(0, originalName.length() - 1) + " 2)";
        } else {
            // Add copy suffix
            return originalName + " (Copy)";
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userPrincipal.getId()));
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
                .owner(file.getUser().getUsername())
                .ownerId(file.getUser().getId())
                .downloadUrl("/api/v1/files/download/" + file.getId())
                .build();
    }
}