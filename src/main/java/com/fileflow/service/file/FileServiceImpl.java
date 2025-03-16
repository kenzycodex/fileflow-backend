package com.fileflow.service.file;

import com.fileflow.dto.request.file.ChunkUploadRequest;
import com.fileflow.dto.request.file.FileUpdateRequest;
import com.fileflow.dto.request.file.FileUploadRequest;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.file.FileResponse;
import com.fileflow.dto.response.file.FileUploadResponse;
import com.fileflow.exception.BadRequestException;
import com.fileflow.exception.ForbiddenException;
import com.fileflow.exception.ResourceNotFoundException;
import com.fileflow.exception.StorageException;
import com.fileflow.model.File;
import com.fileflow.model.Folder;
import com.fileflow.model.StorageChunk;
import com.fileflow.model.User;
import com.fileflow.repository.FileRepository;
import com.fileflow.repository.FolderRepository;
import com.fileflow.repository.StorageChunkRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileServiceImpl implements FileService {

    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final StorageChunkRepository storageChunkRepository;
    private final StorageService storageService;
    private final ActivityService activityService;
    private final QuotaService quotaService;

    // In-memory store for chunked uploads (should be replaced with Redis in production)
    private final Map<String, List<StorageChunk>> chunkedUploads = new HashMap<>();

    @Override
    @Transactional
    public FileUploadResponse uploadFile(FileUploadRequest uploadRequest) throws IOException {
        User currentUser = getCurrentUser();
        MultipartFile file = uploadRequest.getFile();

        if (file.isEmpty()) {
            throw new BadRequestException("Cannot upload empty file");
        }

        // Check file size against quota
        long fileSize = file.getSize();
        if (!quotaService.checkAndReserveQuota(currentUser.getId(), fileSize)) {
            throw new BadRequestException("Insufficient storage quota");
        }

        // Determine parent folder (null for root)
        Folder parentFolder = null;
        if (uploadRequest.getFolderId() != null) {
            parentFolder = folderRepository.findByIdAndUserAndIsDeletedFalse(
                            uploadRequest.getFolderId(), currentUser)
                    .orElseThrow(() -> new ResourceNotFoundException("Folder", "id",
                            uploadRequest.getFolderId()));
        }

        // Sanitize and generate unique filename
        String originalFilename = FileUtils.sanitizeFilename(file.getOriginalFilename());
        String uniqueFilename = FileUtils.generateUniqueFilename(originalFilename);

        // Determine storage directory (user-specific)
        String storageDir = "users/" + currentUser.getId();

        // Store file
        String storagePath = storageService.store(file, uniqueFilename, storageDir);

        // Generate checksum for deduplication
        String checksum = generateChecksum(file);

        // Determine file type
        String fileType = FileUtils.determineFileType(originalFilename);

        // Create file record in database
        File fileEntity = File.builder()
                .filename(originalFilename)
                .originalFilename(originalFilename)
                .storagePath(storagePath)
                .fileSize(fileSize)
                .fileType(fileType)
                .mimeType(file.getContentType())
                .user(currentUser)
                .parentFolder(parentFolder)
                .isFavorite(false)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .lastAccessed(LocalDateTime.now())
                .checksum(checksum)
                .build();

        File savedFile = fileRepository.save(fileEntity);

        // Update user's storage usage
        quotaService.updateStorageUsed(currentUser.getId(), fileSize);

        // Log activity
        activityService.logActivity(
                currentUser.getId(),
                Constants.ACTIVITY_UPLOAD,
                Constants.ITEM_TYPE_FILE,
                savedFile.getId(),
                "Uploaded file: " + savedFile.getFilename()
        );

        // Create download URL
        String downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/files/download/")
                .path(savedFile.getId().toString())
                .toUriString();

        return FileUploadResponse.builder()
                .fileId(savedFile.getId())
                .filename(savedFile.getFilename())
                .originalFilename(savedFile.getOriginalFilename())
                .fileSize(savedFile.getFileSize())
                .fileType(savedFile.getFileType())
                .mimeType(savedFile.getMimeType())
                .parentFolderId(parentFolder != null ? parentFolder.getId() : null)
                .downloadUrl(downloadUrl)
                .build();
    }

    @Override
    public ApiResponse uploadChunk(ChunkUploadRequest chunkRequest) throws IOException {
        User currentUser = getCurrentUser();
        MultipartFile chunkFile = chunkRequest.getChunk();

        if (chunkFile.isEmpty()) {
            throw new BadRequestException("Cannot upload empty chunk");
        }

        // Check if this is the first chunk
        if (chunkRequest.getChunkNumber() == 0) {
            // Check file size against quota
            if (!quotaService.checkAndReserveQuota(currentUser.getId(), chunkRequest.getTotalSize())) {
                throw new BadRequestException("Insufficient storage quota");
            }

            // Generate upload ID if not provided
            if (chunkRequest.getUploadId() == null || chunkRequest.getUploadId().isEmpty()) {
                chunkRequest.setUploadId(UUID.randomUUID().toString());
            }
        }

        // Determine storage directory (user-specific and upload-specific)
        String storageDir = "users/" + currentUser.getId() + "/chunks/" + chunkRequest.getUploadId();

        // Store chunk
        String chunkPath = storageService.store(
                chunkFile,
                "chunk-" + chunkRequest.getChunkNumber(),
                storageDir
        );

        // Create or update chunk record
        StorageChunk chunk = StorageChunk.builder()
                .chunkNumber(chunkRequest.getChunkNumber())
                .uploadId(chunkRequest.getUploadId())
                .userId(currentUser.getId())
                .originalFilename(chunkRequest.getOriginalFilename())
                .storagePath(chunkPath)
                .chunkSize(chunkFile.getSize())
                .totalSize(chunkRequest.getTotalSize())
                .totalChunks(chunkRequest.getTotalChunks())
                .mimeType(chunkFile.getContentType())
                .parentFolderId(chunkRequest.getFolderId())
                .createdAt(LocalDateTime.now())
                .build();

        storageChunkRepository.save(chunk);

        // Check if this is the last chunk
        if (chunkRequest.getChunkNumber() == chunkRequest.getTotalChunks() - 1) {
            // Complete the upload asynchronously
            return ApiResponse.builder()
                    .success(true)
                    .message("All chunks received, processing file")
                    .data(Map.of("uploadId", chunkRequest.getUploadId(), "complete", true))
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        return ApiResponse.builder()
                .success(true)
                .message("Chunk received")
                .data(Map.of("uploadId", chunkRequest.getUploadId(), "complete", false))
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public FileUploadResponse completeChunkedUpload(String uploadId) throws IOException {
        User currentUser = getCurrentUser();

        // Get all chunks for this upload
        List<StorageChunk> chunks = storageChunkRepository.findByUploadIdAndUserId(
                uploadId, currentUser.getId());

        if (chunks.isEmpty()) {
            throw new ResourceNotFoundException("Upload", "id", uploadId);
        }

        // Sort chunks by number
        chunks.sort(Comparator.comparingInt(StorageChunk::getChunkNumber));

        // Check if we have all chunks
        StorageChunk firstChunk = chunks.get(0);
        int totalChunks = firstChunk.getTotalChunks();

        if (chunks.size() != totalChunks) {
            throw new BadRequestException("Missing chunks for upload: " +
                    uploadId + ". Expected " + totalChunks + " but got " + chunks.size());
        }

        // Information from first chunk
        String originalFilename = firstChunk.getOriginalFilename();
        long totalSize = firstChunk.getTotalSize();
        String mimeType = firstChunk.getMimeType();
        Long parentFolderId = firstChunk.getParentFolderId();

        // Determine parent folder
        Folder parentFolder = null;
        if (parentFolderId != null) {
            parentFolder = folderRepository.findByIdAndUserAndIsDeletedFalse(
                            parentFolderId, currentUser)
                    .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", parentFolderId));
        }

        // Sanitize filename
        String sanitizedFilename = FileUtils.sanitizeFilename(originalFilename);

        // Determine file type
        String fileType = FileUtils.determineFileType(sanitizedFilename);

        // Merge chunks and store final file
        String uniqueFilename = FileUtils.generateUniqueFilename(sanitizedFilename);
        String storagePath = storageService.mergeChunks(chunks, uniqueFilename,
                "users/" + currentUser.getId());

        // Create file record
        File file = File.builder()
                .filename(sanitizedFilename)
                .originalFilename(originalFilename)
                .storagePath(storagePath)
                .fileSize(totalSize)
                .fileType(fileType)
                .mimeType(mimeType)
                .user(currentUser)
                .parentFolder(parentFolder)
                .isFavorite(false)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .lastAccessed(LocalDateTime.now())
                .build();

        File savedFile = fileRepository.save(file);

        // Update user's storage usage (quota was already reserved)
        quotaService.confirmQuotaUsage(currentUser.getId(), totalSize);

        // Log activity
        activityService.logActivity(
                currentUser.getId(),
                Constants.ACTIVITY_UPLOAD,
                Constants.ITEM_TYPE_FILE,
                savedFile.getId(),
                "Uploaded file (chunked): " + savedFile.getFilename()
        );

        // Clean up chunks
        deleteChunks(chunks);

        // Create download URL
        String downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/files/download/")
                .path(savedFile.getId().toString())
                .toUriString();

        return FileUploadResponse.builder()
                .fileId(savedFile.getId())
                .filename(savedFile.getFilename())
                .originalFilename(savedFile.getOriginalFilename())
                .fileSize(savedFile.getFileSize())
                .fileType(savedFile.getFileType())
                .mimeType(savedFile.getMimeType())
                .parentFolderId(parentFolder != null ? parentFolder.getId() : null)
                .downloadUrl(downloadUrl)
                .build();
    }

    @Override
    public FileResponse getFile(Long fileId) {
        User currentUser = getCurrentUser();

        File file = fileRepository.findByIdAndUserAndIsDeletedFalse(fileId, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("File", "id", fileId));

        // Update last accessed time
        file.setLastAccessed(LocalDateTime.now());
        fileRepository.save(file);

        return mapFileToFileResponse(file);
    }

    @Override
    public Resource loadFileAsResource(Long fileId) {
        User currentUser = getCurrentUser();

        File file = fileRepository.findByIdAndUserAndIsDeletedFalse(fileId, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("File", "id", fileId));

        // Update last accessed time
        file.setLastAccessed(LocalDateTime.now());
        fileRepository.save(file);

        // Log activity
        activityService.logActivity(
                currentUser.getId(),
                Constants.ACTIVITY_DOWNLOAD,
                Constants.ITEM_TYPE_FILE,
                fileId,
                "Downloaded file: " + file.getFilename()
        );

        return storageService.loadAsResource(file.getStoragePath());
    }

    @Override
    @Transactional
    public FileResponse updateFile(Long fileId, FileUpdateRequest updateRequest) {
        User currentUser = getCurrentUser();

        File file = fileRepository.findByIdAndUserAndIsDeletedFalse(fileId, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("File", "id", fileId));

        // Update filename if provided
        if (updateRequest.getFilename() != null && !updateRequest.getFilename().isEmpty() &&
                !updateRequest.getFilename().equals(file.getFilename())) {

            // Sanitize filename
            String sanitizedFilename = FileUtils.sanitizeFilename(updateRequest.getFilename());

            // Check if file with same name already exists in the same folder
            boolean fileExists = fileRepository.findByUserAndParentFolderAndIsDeletedFalse(
                            currentUser, file.getParentFolder())
                    .stream()
                    .filter(f -> !f.getId().equals(fileId)) // Exclude the current file
                    .anyMatch(f -> f.getFilename().equals(sanitizedFilename));

            if (fileExists) {
                throw new BadRequestException("File with name '" + sanitizedFilename +
                        "' already exists in this location");
            }

            file.setFilename(sanitizedFilename);
        }

        // Update parent folder if provided
        if (updateRequest.getParentFolderId() != null &&
                !updateRequest.getParentFolderId().equals(
                        file.getParentFolder() != null ? file.getParentFolder().getId() : null)) {

            Folder newParentFolder = null;
            if (updateRequest.getParentFolderId() > 0) {
                newParentFolder = folderRepository.findByIdAndUserAndIsDeletedFalse(
                                updateRequest.getParentFolderId(), currentUser)
                        .orElseThrow(() -> new ResourceNotFoundException("Folder", "id",
                                updateRequest.getParentFolderId()));
            }

            // Check if file with same name already exists in the new folder
            boolean fileExists = fileRepository.findByUserAndParentFolderAndIsDeletedFalse(
                            currentUser, newParentFolder)
                    .stream()
                    .anyMatch(f -> f.getFilename().equals(file.getFilename()));

            if (fileExists) {
                throw new BadRequestException("File with name '" + file.getFilename() +
                        "' already exists in the destination folder");
            }

            file.setParentFolder(newParentFolder);
        }

        // Update favorite status if provided
        if (updateRequest.getIsFavorite() != null) {
            file.setFavorite(updateRequest.getIsFavorite());
        }

        file.setUpdatedAt(LocalDateTime.now());
        File updatedFile = fileRepository.save(file);

        // Log activity
        activityService.logActivity(
                currentUser.getId(),
                Constants.ACTIVITY_UPDATE_FILE,
                Constants.ITEM_TYPE_FILE,
                fileId,
                "Updated file: " + file.getFilename()
        );

        return mapFileToFileResponse(updatedFile);
    }

    @Override
    @Transactional
    public ApiResponse deleteFile(Long fileId) {
        User currentUser = getCurrentUser();

        File file = fileRepository.findByIdAndUserAndIsDeletedFalse(fileId, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("File", "id", fileId));

        // Move to trash (soft delete)
        file.setDeleted(true);
        file.setDeletedAt(LocalDateTime.now());
        fileRepository.save(file);

        // Log activity
        activityService.logActivity(
                currentUser.getId(),
                Constants.ACTIVITY_DELETE,
                Constants.ITEM_TYPE_FILE,
                fileId,
                "Moved file to trash: " + file.getFilename()
        );

        return ApiResponse.builder()
                .success(true)
                .message("File moved to trash")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public ApiResponse restoreFile(Long fileId) {
        User currentUser = getCurrentUser();

        // Find the deleted file
        File file = fileRepository.findById(fileId)
                .filter(f -> f.getUser().getId().equals(currentUser.getId()) && f.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("File", "id", fileId));

        // Check if parent folder is deleted
        if (file.getParentFolder() != null && file.getParentFolder().isDeleted()) {
            // Restore to root if parent is deleted
            file.setParentFolder(null);
        }

        // Restore file
        file.setDeleted(false);
        file.setDeletedAt(null);
        fileRepository.save(file);

        // Log activity
        activityService.logActivity(
                currentUser.getId(),
                Constants.ACTIVITY_RESTORE,
                Constants.ITEM_TYPE_FILE,
                fileId,
                "Restored file from trash: " + file.getFilename()
        );

        return ApiResponse.builder()
                .success(true)
                .message("File restored from trash")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public ApiResponse permanentDeleteFile(Long fileId) {
        User currentUser = getCurrentUser();

        // Find the file (deleted or not)
        File file = fileRepository.findById(fileId)
                .filter(f -> f.getUser().getId().equals(currentUser.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("File", "id", fileId));

        // Get file info for logging
        String filename = file.getFilename();
        long fileSize = file.getFileSize();

        // Delete file from database
        fileRepository.delete(file);

        // Update user's storage usage
        quotaService.releaseStorage(currentUser.getId(), fileSize);

        // Log activity (file ID will no longer be valid after deletion)
        activityService.logActivity(
                currentUser.getId(),
                Constants.ACTIVITY_PERMANENT_DELETE,
                Constants.ITEM_TYPE_FILE,
                null,
                "Permanently deleted file: " + filename
        );

        // Don't delete from storage immediately (scheduled task will clean up)
        // This prevents deletion of files that might be referenced by other users (deduplication)

        return ApiResponse.builder()
                .success(true)
                .message("File permanently deleted")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public FileResponse moveFile(Long fileId, Long destinationFolderId) {
        User currentUser = getCurrentUser();

        File file = fileRepository.findByIdAndUserAndIsDeletedFalse(fileId, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("File", "id", fileId));

        // Determine destination folder (null for root)
        Folder destinationFolder = null;
        if (destinationFolderId != null && destinationFolderId > 0) {
            destinationFolder = folderRepository.findByIdAndUserAndIsDeletedFalse(
                            destinationFolderId, currentUser)
                    .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", destinationFolderId));
        }

        // Check if already in the destination folder
        if ((file.getParentFolder() == null && destinationFolder == null) ||
                (file.getParentFolder() != null && file.getParentFolder().getId().equals(destinationFolderId))) {
            throw new BadRequestException("File is already in the destination folder");
        }

        // Check if file with same name already exists in destination
        boolean fileExists = fileRepository.findByUserAndParentFolderAndIsDeletedFalse(
                        currentUser, destinationFolder)
                .stream()
                .anyMatch(f -> f.getFilename().equals(file.getFilename()));

        if (fileExists) {
            throw new BadRequestException("File with name '" + file.getFilename() +
                    "' already exists in the destination folder");
        }

        // Move file
        file.setParentFolder(destinationFolder);
        file.setUpdatedAt(LocalDateTime.now());
        File movedFile = fileRepository.save(file);

        // Log activity
        activityService.logActivity(
                currentUser.getId(),
                Constants.ACTIVITY_MOVE,
                Constants.ITEM_TYPE_FILE,
                fileId,
                "Moved file: " + file.getFilename()
        );

        return mapFileToFileResponse(movedFile);
    }

    @Override
    @Transactional
    public FileResponse copyFile(Long fileId, Long destinationFolderId) {
        User currentUser = getCurrentUser();

        File sourceFile = fileRepository.findByIdAndUserAndIsDeletedFalse(fileId, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("File", "id", fileId));

        // Determine destination folder (null for root)
        Folder destinationFolder = null;
        if (destinationFolderId != null && destinationFolderId > 0) {
            destinationFolder = folderRepository.findByIdAndUserAndIsDeletedFalse(
                            destinationFolderId, currentUser)
                    .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", destinationFolderId));
        }

        // Determine new filename (handle duplicates)
        String newFilename = sourceFile.getFilename();
        boolean nameExists = fileRepository.findByUserAndParentFolderAndIsDeletedFalse(
                        currentUser, destinationFolder)
                .stream()
                .anyMatch(f -> f.getFilename().equals(newFilename));

        if (nameExists) {
            newFilename = generateCopyName(newFilename);
        }

        // Create new file record (pointing to same storage object)
        File newFile = File.builder()
                .filename(newFilename)
                .originalFilename(sourceFile.getOriginalFilename())
                .storagePath(sourceFile.getStoragePath())
                .fileSize(sourceFile.getFileSize())
                .fileType(sourceFile.getFileType())
                .mimeType(sourceFile.getMimeType())
                .user(currentUser)
                .parentFolder(destinationFolder)
                .isFavorite(false)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .lastAccessed(LocalDateTime.now())
                .checksum(sourceFile.getChecksum())
                .build();

        File savedFile = fileRepository.save(newFile);

        // Update user's storage usage
        quotaService.updateStorageUsed(currentUser.getId(), sourceFile.getFileSize());

        // Log activity
        activityService.logActivity(
                currentUser.getId(),
                Constants.ACTIVITY_COPY,
                Constants.ITEM_TYPE_FILE,
                savedFile.getId(),
                "Copied file: " + sourceFile.getFilename()
        );

        return mapFileToFileResponse(savedFile);
    }

    @Override
    @Transactional
    public FileResponse toggleFavorite(Long fileId) {
        User currentUser = getCurrentUser();

        File file = fileRepository.findByIdAndUserAndIsDeletedFalse(fileId, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("File", "id", fileId));

        // Toggle favorite status
        file.setFavorite(!file.isFavorite());
        file.setUpdatedAt(LocalDateTime.now());
        File updatedFile = fileRepository.save(file);

        // Log activity
        String action = updatedFile.isFavorite() ? "Added file to favorites" : "Removed file from favorites";
        activityService.logActivity(
                currentUser.getId(),
                Constants.ACTIVITY_UPDATE_FILE,
                Constants.ITEM_TYPE_FILE,
                fileId,
                action + ": " + file.getFilename()
        );

        return mapFileToFileResponse(updatedFile);
    }

    @Override
    public List<FileResponse> getFilesInFolder(Long folderId) {
        User currentUser = getCurrentUser();

        // Determine folder (null for root)
        Folder folder = null;
        if (folderId != null && folderId > 0) {
            folder = folderRepository.findByIdAndUserAndIsDeletedFalse(folderId, currentUser)
                    .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", folderId));
        }

        List<File> files = fileRepository.findByUserAndParentFolderAndIsDeletedFalse(currentUser, folder);

        return files.stream()
                .map(this::mapFileToFileResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<FileResponse> getRecentFiles(int limit) {
        User currentUser = getCurrentUser();

        // Get recent files
        Pageable pageable = PageRequest.of(0, limit, Sort.Direction.DESC, "lastAccessed");
        Page<File> recentFiles = fileRepository.findRecentFiles(currentUser, pageable);

        return recentFiles.getContent().stream()
                .map(this::mapFileToFileResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<FileResponse> getFavoriteFiles() {
        User currentUser = getCurrentUser();

        // Get favorite files
        Pageable pageable = PageRequest.of(0, 100, Sort.Direction.DESC, "lastAccessed");
        Page<File> favoriteFiles = fileRepository.findFavorites(currentUser, pageable);

        return favoriteFiles.getContent().stream()
                .map(this::mapFileToFileResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<FileResponse> searchFiles(String keyword) {
        User currentUser = getCurrentUser();

        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }

        // Search files by name
        Pageable pageable = PageRequest.of(0, 100, Sort.Direction.DESC, "lastAccessed");
        Page<File> searchResults = fileRepository.searchByFilename(currentUser, keyword, pageable);

        return searchResults.getContent().stream()
                .map(this::mapFileToFileResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public int cleanupDeletedFiles(int daysInTrash) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysInTrash);
        List<File> filesToDelete = fileRepository.findByIsDeletedTrueAndDeletedAtBefore(cutoffDate);

        int count = 0;
        for (File file : filesToDelete) {
            try {
                // Keep track of user and file size for quota update
                Long userId = file.getUser().getId();
                Long fileSize = file.getFileSize();

                // Delete from database
                fileRepository.delete(file);

                // Update user's storage quota
                quotaService.releaseStorage(userId, fileSize);

                // Only delete from storage if no other files are using the same storage path
                if (!fileRepository.existsByStoragePath(file.getStoragePath())) {
                    storageService.delete(file.getStoragePath());
                }

                count++;
            } catch (Exception e) {
                log.error("Error deleting file: {}", file.getId(), e);
            }
        }

        log.info("Deleted {} files from trash", count);
        return count;
    }

    // Helper methods

    private String generateChecksum(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(file.getBytes());

            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            log.error("Error generating checksum", e);
            return null;
        }
    }

    private void deleteChunks(List<StorageChunk> chunks) {
        for (StorageChunk chunk : chunks) {
            try {
                // Delete from storage
                storageService.delete(chunk.getStoragePath());

                // Delete from database
                storageChunkRepository.delete(chunk);
            } catch (Exception e) {
                log.error("Error deleting chunk: {}", chunk.getId(), e);
            }
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

    private FileResponse mapFileToFileResponse(File file) {
        String downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/files/download/")
                .path(file.getId().toString())
                .toUriString();

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
                .downloadUrl(downloadUrl)
                .createdAt(file.getCreatedAt())
                .updatedAt(file.getUpdatedAt())
                .lastAccessed(file.getLastAccessed())
                .owner(file.getUser().getUsername())
                .ownerId(file.getUser().getId())
                .build();
    }
}