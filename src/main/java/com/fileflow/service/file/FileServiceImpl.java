package com.fileflow.service.file;

import com.fileflow.dto.request.file.ChunkUploadRequest;
import com.fileflow.dto.request.file.FileUpdateRequest;
import com.fileflow.dto.request.file.FileUploadRequest;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.common.PageResponse;
import com.fileflow.dto.response.file.FileResponse;
import com.fileflow.dto.response.file.FileUploadResponse;
import com.fileflow.exception.BadRequestException;
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
import com.fileflow.service.storage.EnhancedStorageService;
import com.fileflow.service.storage.StorageServiceFactory;
import com.fileflow.service.thumbnail.ThumbnailService;
import com.fileflow.service.websocket.WebSocketService;
import com.fileflow.util.Constants;
import com.fileflow.util.FileUtils;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementation of the FileService interface with improved error handling,
 * performance optimizations, and additional features including WebSocket integration
 */
@Service
@Slf4j
public class FileServiceImpl implements FileService {

    // Repositories
    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final StorageChunkRepository storageChunkRepository;

    // Services
    private final EnhancedStorageService storageService;
    private final ActivityService activityService;
    private final QuotaService quotaService;
    private final ThumbnailService thumbnailService;
    private final WebSocketService webSocketService;

    // Configuration
    @Value("${fileflow.storage.chunk-size:5MB}")
    private String chunkSizeStr;

    @Value("${fileflow.storage.enable-deduplication:true}")
    private boolean enableDeduplication;

    @Value("${fileflow.storage.chunk-expiry-minutes:60}")
    private int chunkExpiryMinutes;

    // Track upload progress (uploadId -> progress percentage)
    private final Map<String, Integer> uploadProgressMap = new ConcurrentHashMap<>();

    // File locks for concurrent operations
    private final Map<Long, Object> fileLocks = new ConcurrentHashMap<>();

    @Autowired
    public FileServiceImpl(
            FileRepository fileRepository,
            FolderRepository folderRepository,
            UserRepository userRepository,
            StorageChunkRepository storageChunkRepository,
            StorageServiceFactory storageServiceFactory,
            ActivityService activityService,
            QuotaService quotaService,
            ThumbnailService thumbnailService,
            WebSocketService webSocketService) {
        this.fileRepository = fileRepository;
        this.folderRepository = folderRepository;
        this.userRepository = userRepository;
        this.storageChunkRepository = storageChunkRepository;
        this.storageService = storageServiceFactory.getStorageService();
        this.activityService = activityService;
        this.quotaService = quotaService;
        this.thumbnailService = thumbnailService;
        this.webSocketService = webSocketService;
    }

    @Override
    @Transactional
    @Timed("file.upload")
    public FileUploadResponse uploadFile(FileUploadRequest uploadRequest) throws IOException {
        User currentUser = getCurrentUser();
        MultipartFile file = uploadRequest.getFile();

        // Validate file
        validateFileForUpload(file);

        // Check file size against quota
        long fileSize = file.getSize();
        if (!quotaService.checkAndReserveQuota(currentUser.getId(), fileSize)) {
            throw new BadRequestException("Insufficient storage quota");
        }

        try {
            // Determine parent folder (null for root)
            Folder parentFolder = resolveParentFolder(uploadRequest.getFolderId(), currentUser);

            // Sanitize and handle filename
            String originalFilename = FileUtils.sanitizeFilename(file.getOriginalFilename());
            String uniqueFilename = FileUtils.generateUniqueFilename(originalFilename);

            // Handle duplicate filenames
            originalFilename = handleDuplicateFilename(currentUser, parentFolder, originalFilename, uploadRequest.getOverwrite());

            // Determine storage directory (user-specific)
            String storageDir = "users/" + currentUser.getId();

            // Store file
            String storagePath = storageService.store(file, uniqueFilename, storageDir);

            // Content validation can be performed here (virus scan, etc.)

            // Handle deduplication if enabled
            String checksum = null;
            if (enableDeduplication) {
                // Generate checksum for deduplication
                checksum = storageService.computeHash(file);

                // Check for deduplication
                if (checksum != null) {
                    checkForDuplicateFiles(currentUser, checksum, originalFilename);
                }
            }

            // Determine file type
            String fileType = FileUtils.determineFileType(originalFilename);

            // Create file record in database
            File fileEntity = createFileEntity(
                    currentUser,
                    parentFolder,
                    originalFilename,
                    file.getOriginalFilename(),
                    storagePath,
                    fileSize,
                    fileType,
                    file.getContentType(),
                    checksum
            );

            File savedFile = fileRepository.save(fileEntity);

            // Update user's storage usage
            quotaService.confirmQuotaUsage(currentUser.getId(), fileSize);

            // Log activity
            logFileActivity(
                    currentUser.getId(),
                    Constants.ACTIVITY_UPLOAD,
                    savedFile.getId(),
                    "Uploaded file: " + savedFile.getFilename()
            );

            // Generate thumbnail asynchronously
            generateThumbnailAsync(savedFile.getId());

            // Create response
            FileUploadResponse response = createFileUploadResponse(savedFile, parentFolder);

            // Send WebSocket notification
            notifyFileUpload(mapFileToFileResponse(savedFile));

            return response;
        } catch (Exception e) {
            // Release reserved quota on failure
            quotaService.releaseQuotaReservation(currentUser.getId(), fileSize);

            // Rethrow as appropriate exception type
            if (e instanceof BadRequestException) {
                throw e;
            } else if (e instanceof ResourceNotFoundException) {
                throw e;
            } else {
                log.error("Error uploading file", e);
                throw new StorageException("Failed to store file", e);
            }
        }
    }

    /**
     * Validate file for upload
     */
    private void validateFileForUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Cannot upload empty file");
        }

        // Check for file size limits
        FileUtils.validateFileSize(file, Constants.MAX_FILE_SIZE);

        // Verify filename is valid
        if (file.getOriginalFilename() == null || file.getOriginalFilename().trim().isEmpty()) {
            throw new BadRequestException("Invalid filename");
        }

        // Basic content type validation could be added here
    }

    /**
     * Resolve parent folder
     */
    private Folder resolveParentFolder(Long folderId, User user) {
        if (folderId == null) {
            return null;
        }

        return folderRepository.findByIdAndUserAndIsDeletedFalse(folderId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", folderId));
    }

    /**
     * Handle duplicate filenames
     */
    private String handleDuplicateFilename(User user, Folder parentFolder, String originalFilename, Boolean overwrite) {
        // Check for duplicate filename in the same folder if overwrite not specified
        if (overwrite == null || !overwrite) {
            String finalOriginalFilename = originalFilename;
            boolean fileExists = fileRepository.findByUserAndParentFolderAndIsDeletedFalse(user, parentFolder)
                    .stream()
                    .anyMatch(f -> f.getFilename().equals(finalOriginalFilename));

            if (fileExists) {
                // Generate a new name to avoid duplication
                return generateCopyName(originalFilename);
            }
        }

        return originalFilename;
    }

    /**
     * Check for duplicate files using checksum
     */
    private void checkForDuplicateFiles(User user, String checksum, String filename) {
        List<File> existingFiles = fileRepository.findByUserAndChecksum(user, checksum);
        if (!existingFiles.isEmpty()) {
            log.info("Duplicate file detected for user {}: {}", user.getId(), filename);
            // We could implement a more sophisticated deduplication strategy here
        }
    }

    /**
     * Create the file entity
     */
    private File createFileEntity(User user, Folder parentFolder, String filename, String originalFilename,
                                  String storagePath, long fileSize, String fileType, String mimeType, String checksum) {
        return File.builder()
                .filename(filename)
                .originalFilename(originalFilename)
                .storagePath(storagePath)
                .fileSize(fileSize)
                .fileType(fileType)
                .mimeType(mimeType)
                .user(user)
                .parentFolder(parentFolder)
                .isFavorite(false)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .lastAccessed(LocalDateTime.now())
                .checksum(checksum)
                .build();
    }

    /**
     * Create file upload response
     */
    private FileUploadResponse createFileUploadResponse(File file, Folder parentFolder) {
        String downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/files/download/")
                .path(file.getId().toString())
                .toUriString();

        return FileUploadResponse.builder()
                .fileId(file.getId())
                .filename(file.getFilename())
                .originalFilename(file.getOriginalFilename())
                .fileSize(file.getFileSize())
                .fileType(file.getFileType())
                .mimeType(file.getMimeType())
                .parentFolderId(parentFolder != null ? parentFolder.getId() : null)
                .downloadUrl(downloadUrl)
                .build();
    }

    @Override
    @Timed("file.upload.chunk")
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

        try {
            // Determine storage directory (user-specific and upload-specific)
            String storageDir = "users/" + currentUser.getId() + "/chunks/" + chunkRequest.getUploadId();

            // Store chunk
            String chunkPath = storageService.store(
                    chunkFile,
                    "chunk-" + chunkRequest.getChunkNumber(),
                    storageDir
            );

            // Calculate expiry time for chunks
            LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(chunkExpiryMinutes);

            // Create or update chunk record
            StorageChunk chunk = StorageChunk.builder()
                    .uploadId(chunkRequest.getUploadId())
                    .chunkNumber(chunkRequest.getChunkNumber())
                    .totalChunks(chunkRequest.getTotalChunks())
                    .userId(currentUser.getId())
                    .originalFilename(chunkRequest.getOriginalFilename())
                    .storagePath(chunkPath)
                    .chunkSize(chunkFile.getSize())
                    .totalSize(chunkRequest.getTotalSize())
                    .mimeType(chunkFile.getContentType())
                    .parentFolderId(chunkRequest.getFolderId())
                    .createdAt(LocalDateTime.now())
                    .expiresAt(expiryTime)
                    .build();

            storageChunkRepository.save(chunk);

            // Update progress tracking
            updateUploadProgress(chunkRequest.getUploadId(), chunkRequest.getChunkNumber(), chunkRequest.getTotalChunks());

            // Check if this is the last chunk
            if (chunkRequest.getChunkNumber() == chunkRequest.getTotalChunks() - 1) {
                // Complete the upload asynchronously
                completeChunkedUploadAsync(chunkRequest.getUploadId());

                return ApiResponse.builder()
                        .success(true)
                        .message("All chunks received, processing file")
                        .data(Map.of(
                                "uploadId", chunkRequest.getUploadId(),
                                "complete", true,
                                "progress", 100
                        ))
                        .timestamp(LocalDateTime.now())
                        .build();
            }

            return ApiResponse.builder()
                    .success(true)
                    .message("Chunk received")
                    .data(Map.of(
                            "uploadId", chunkRequest.getUploadId(),
                            "complete", false,
                            "progress", getUploadProgress(chunkRequest.getUploadId())
                    ))
                    .timestamp(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("Error uploading chunk", e);

            // Release quota for failed first chunk
            if (chunkRequest.getChunkNumber() == 0) {
                quotaService.releaseQuotaReservation(currentUser.getId(), chunkRequest.getTotalSize());
            }

            throw new StorageException("Failed to store chunk", e);
        }
    }

    /**
     * Update upload progress tracking
     */
    private void updateUploadProgress(String uploadId, int chunkNumber, int totalChunks) {
        int progress = (int) (((float) (chunkNumber + 1) / totalChunks) * 100);
        uploadProgressMap.put(uploadId, progress);
    }

    /**
     * Get upload progress for an upload ID
     */
    private int getUploadProgress(String uploadId) {
        return uploadProgressMap.getOrDefault(uploadId, 0);
    }

    @Async
    protected void completeChunkedUploadAsync(String uploadId) {
        try {
            completeChunkedUpload(uploadId);
            // Clean up progress tracking
            uploadProgressMap.remove(uploadId);
        } catch (Exception e) {
            log.error("Error completing chunked upload: {}", uploadId, e);
        }
    }

    @Override
    @Transactional
    @Timed("file.upload.complete")
    @Retryable(
            value = {StorageException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public FileUploadResponse completeChunkedUpload(String uploadId) throws IOException {
        User currentUser = getCurrentUser();

        // Get all chunks for this upload
        List<StorageChunk> chunks = storageChunkRepository.findByUploadIdAndUserId(uploadId, currentUser.getId());

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

        try {
            // Determine parent folder
            Folder parentFolder = resolveParentFolder(parentFolderId, currentUser);

            // Sanitize filename
            String sanitizedFilename = FileUtils.sanitizeFilename(originalFilename);

            // Determine file type
            String fileType = FileUtils.determineFileType(sanitizedFilename);

            // Generate unique filename
            String uniqueFilename = FileUtils.generateUniqueFilename(sanitizedFilename);

            // Merge chunks and store final file
            String storagePath = storageService.mergeChunks(chunks, uniqueFilename,
                    "users/" + currentUser.getId());

            // Create file record
            File file = createFileEntity(
                    currentUser,
                    parentFolder,
                    sanitizedFilename,
                    originalFilename,
                    storagePath,
                    totalSize,
                    fileType,
                    mimeType,
                    null
            );

            File savedFile = fileRepository.save(file);

            // Update user's storage usage (quota was already reserved)
            quotaService.confirmQuotaUsage(currentUser.getId(), totalSize);

            // Log activity
            logFileActivity(
                    currentUser.getId(),
                    Constants.ACTIVITY_UPLOAD,
                    savedFile.getId(),
                    "Uploaded file (chunked): " + savedFile.getFilename()
            );

            // Clean up chunks
            deleteChunks(chunks);

            // Generate thumbnail asynchronously
            generateThumbnailAsync(savedFile.getId());

            // Create response
            FileUploadResponse response = createFileUploadResponse(savedFile, parentFolder);

            // Send WebSocket notification
            notifyFileUpload(mapFileToFileResponse(savedFile));

            return response;
        } catch (Exception e) {
            log.error("Error completing chunked upload", e);

            // Release reserved quota on failure
            quotaService.releaseQuotaReservation(currentUser.getId(), totalSize);

            // Rethrow as appropriate exception type
            if (e instanceof BadRequestException) {
                throw e;
            } else if (e instanceof ResourceNotFoundException) {
                throw e;
            } else {
                throw new StorageException("Failed to complete chunked upload", e);
            }
        }
    }

    @Override
    @Timed("file.get")
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
    @Timed("file.download")
    public Resource loadFileAsResource(Long fileId) {
        User currentUser = getCurrentUser();

        File file = fileRepository.findByIdAndUserAndIsDeletedFalse(fileId, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("File", "id", fileId));

        try {
            // Update last accessed time
            file.setLastAccessed(LocalDateTime.now());
            fileRepository.save(file);

            // Log activity
            logFileActivity(
                    currentUser.getId(),
                    Constants.ACTIVITY_DOWNLOAD,
                    fileId,
                    "Downloaded file: " + file.getFilename()
            );

            return storageService.loadAsResource(file.getStoragePath());
        } catch (Exception e) {
            log.error("Error loading file: {}", fileId, e);
            throw new StorageException("Failed to load file", e);
        }
    }

    /**
     * Stream file content for efficient downloading of large files
     */
    public InputStream streamFileContent(Long fileId) {
        User currentUser = getCurrentUser();

        File file = fileRepository.findByIdAndUserAndIsDeletedFalse(fileId, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("File", "id", fileId));

        try {
            // Update last accessed time
            file.setLastAccessed(LocalDateTime.now());
            fileRepository.save(file);

            // Log activity
            logFileActivity(
                    currentUser.getId(),
                    Constants.ACTIVITY_DOWNLOAD,
                    fileId,
                    "Streamed file: " + file.getFilename()
            );

            return storageService.getInputStream(file.getStoragePath());
        } catch (Exception e) {
            log.error("Error streaming file: {}", fileId, e);
            throw new StorageException("Failed to stream file", e);
        }
    }

    @Override
    @Transactional
    @Timed("file.update")
    public FileResponse updateFile(Long fileId, FileUpdateRequest updateRequest) {
        User currentUser = getCurrentUser();

        // Get lock for this file to prevent concurrent modifications
        Object fileLock = fileLocks.computeIfAbsent(fileId, k -> new Object());

        synchronized (fileLock) {
            File file = fileRepository.findByIdAndUserAndIsDeletedFalse(fileId, currentUser)
                    .orElseThrow(() -> new ResourceNotFoundException("File", "id", fileId));

            boolean updated = false;

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
                updated = true;
            }

            // Update parent folder if provided
            Long oldFolderId = file.getParentFolder() != null ? file.getParentFolder().getId() : null;
            if (updateRequest.getParentFolderId() != null &&
                    !updateRequest.getParentFolderId().equals(oldFolderId)) {

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
                updated = true;
            }

            // Update favorite status if provided
            if (updateRequest.getIsFavorite() != null && file.isFavorite() != updateRequest.getIsFavorite()) {
                file.setFavorite(updateRequest.getIsFavorite());
                updated = true;
            }

            if (updated) {
                file.setUpdatedAt(LocalDateTime.now());
                File updatedFile = fileRepository.save(file);

                // Log activity
                logFileActivity(
                        currentUser.getId(),
                        Constants.ACTIVITY_UPDATE_FILE,
                        fileId,
                        "Updated file: " + file.getFilename()
                );

                FileResponse response = mapFileToFileResponse(updatedFile);

                // Send WebSocket notification
                notifyFileUpdate(response);

                return response;
            }

            return mapFileToFileResponse(file);
        }
    }

    @Override
    @Transactional
    @Timed("file.delete")
    public ApiResponse deleteFile(Long fileId) {
        User currentUser = getCurrentUser();

        File file = fileRepository.findByIdAndUserAndIsDeletedFalse(fileId, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("File", "id", fileId));

        // Store needed info before soft delete
        Long parentFolderId = file.getParentFolder() != null ? file.getParentFolder().getId() : null;
        String fileName = file.getFilename();

        // Move to trash (soft delete)
        file.setDeleted(true);
        file.setDeletedAt(LocalDateTime.now());
        fileRepository.save(file);

        // Log activity
        logFileActivity(
                currentUser.getId(),
                Constants.ACTIVITY_DELETE,
                fileId,
                "Moved file to trash: " + file.getFilename()
        );

        // Notify via WebSocket
        notifyFileDelete(fileId, currentUser.getId(), fileName, parentFolderId);

        return ApiResponse.builder()
                .success(true)
                .message("File moved to trash")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    @Timed("file.restore")
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
        logFileActivity(
                currentUser.getId(),
                Constants.ACTIVITY_RESTORE,
                fileId,
                "Restored file from trash: " + file.getFilename()
        );

        // Notify via WebSocket
        notifyFileUpload(mapFileToFileResponse(file));

        return ApiResponse.builder()
                .success(true)
                .message("File restored from trash")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    @Timed("file.permanent-delete")
    public ApiResponse permanentDeleteFile(Long fileId) {
        User currentUser = getCurrentUser();

        // Find the file (deleted or not)
        File file = fileRepository.findById(fileId)
                .filter(f -> f.getUser().getId().equals(currentUser.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("File", "id", fileId));

        // Get file info for logging
        String filename = file.getFilename();
        long fileSize = file.getFileSize();
        String storagePath = file.getStoragePath();
        String thumbnailPath = file.getThumbnailPath();
        Long parentFolderId = file.getParentFolder() != null ? file.getParentFolder().getId() : null;

        try {
            // Delete file from database
            fileRepository.delete(file);

            // Update user's storage usage
            quotaService.releaseStorage(currentUser.getId(), fileSize);

            // Log activity (file ID will no longer be valid after deletion)
            logFileActivity(
                    currentUser.getId(),
                    Constants.ACTIVITY_PERMANENT_DELETE,
                    null,
                    "Permanently deleted file: " + filename
            );

            // Notify via WebSocket
            notifyFileDelete(fileId, currentUser.getId(), filename, parentFolderId);

            // Check if any other files reference the same storage path (deduplication)
            if (!fileRepository.existsByStoragePath(storagePath)) {
                // If no other file references this storage path, delete the file from storage
                storageService.delete(storagePath);
            }

            // Delete thumbnail if exists
            if (thumbnailPath != null && !thumbnailPath.isEmpty()) {
                storageService.delete(thumbnailPath);
            }

            // Remove file lock if exists
            fileLocks.remove(fileId);

            return ApiResponse.builder()
                    .success(true)
                    .message("File permanently deleted")
                    .timestamp(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("Error permanently deleting file", e);
            throw new StorageException("Failed to permanently delete file", e);
        }
    }

    @Override
    @Transactional
    @Timed("file.move")
    public FileResponse moveFile(Long fileId, Long destinationFolderId) {
        User currentUser = getCurrentUser();

        // Get lock for this file to prevent concurrent modifications
        Object fileLock = fileLocks.computeIfAbsent(fileId, k -> new Object());

        synchronized (fileLock) {
            File file = fileRepository.findByIdAndUserAndIsDeletedFalse(fileId, currentUser)
                    .orElseThrow(() -> new ResourceNotFoundException("File", "id", fileId));

            Long oldFolderId = file.getParentFolder() != null ? file.getParentFolder().getId() : null;

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
            logFileActivity(
                    currentUser.getId(),
                    Constants.ACTIVITY_MOVE,
                    fileId,
                    "Moved file: " + file.getFilename()
            );

            FileResponse response = mapFileToFileResponse(movedFile);

            // Notify via WebSocket
            notifyFileMove(response, oldFolderId);

            return response;
        }
    }

    @Override
    @Transactional
    @Timed("file.copy")
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
        String finalNewFilename = newFilename;
        boolean nameExists = fileRepository.findByUserAndParentFolderAndIsDeletedFalse(
                        currentUser, destinationFolder)
                .stream()
                .anyMatch(f -> f.getFilename().equals(finalNewFilename));

        if (nameExists) {
            newFilename = generateCopyName(newFilename);
        }

        try {
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
            logFileActivity(
                    currentUser.getId(),
                    Constants.ACTIVITY_COPY,
                    savedFile.getId(),
                    "Copied file: " + sourceFile.getFilename()
            );

            // Generate thumbnail for the new file
            if (sourceFile.getThumbnailPath() != null && !sourceFile.getThumbnailPath().isEmpty()) {
                // Copy thumbnail
                generateThumbnailAsync(savedFile.getId());
            }

            FileResponse response = mapFileToFileResponse(savedFile);

            // Notify via WebSocket
            notifyFileUpload(response);

            return response;
        } catch (Exception e) {
            log.error("Error copying file", e);
            throw new StorageException("Failed to copy file", e);
        }
    }

    @Override
    @Transactional
    @Timed("file.toggle-favorite")
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
        logFileActivity(
                currentUser.getId(),
                Constants.ACTIVITY_UPDATE_FILE,
                fileId,
                action + ": " + file.getFilename()
        );

        FileResponse response = mapFileToFileResponse(updatedFile);

        // Notify via WebSocket
        notifyFileUpdate(response);

        return response;
    }

    @Override
    @Timed("file.list.paginated")
    public PageResponse<FileResponse> getFilesInFolder(Long folderId, Pageable pageable) {
        User currentUser = getCurrentUser();

        // Determine folder (null for root)
        Folder folder = null;
        if (folderId != null && folderId > 0) {
            folder = folderRepository.findByIdAndUserAndIsDeletedFalse(folderId, currentUser)
                    .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", folderId));
        }

        // Get paginated files
        Page<File> filesPage = fileRepository.findByUserAndParentFolderAndIsDeletedFalse(
                currentUser, folder, pageable);

        // Map to response DTOs
        Page<FileResponse> fileResponsePage = filesPage.map(this::mapFileToFileResponse);

        // Create page response
        return PageResponse.fromPage(fileResponsePage);
    }

    @Override
    @Deprecated
    @Timed("file.list")
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
    @Timed("file.recent.paginated")
    public PageResponse<FileResponse> getRecentFiles(Pageable pageable) {
        User currentUser = getCurrentUser();

        // Get recent files with pagination
        Page<File> recentFilesPage = fileRepository.findRecentFiles(currentUser, pageable);

        // Map to response DTOs
        Page<FileResponse> fileResponsePage = recentFilesPage.map(this::mapFileToFileResponse);

        // Create page response
        return PageResponse.fromPage(fileResponsePage);
    }

    @Override
    @Deprecated
    @Timed("file.recent")
    public List<FileResponse> getRecentFiles(int limit) {
        User currentUser = getCurrentUser();

        // Get recent files with limit
        Pageable pageable = PageRequest.of(0, limit, Sort.Direction.DESC, "lastAccessed");
        Page<File> recentFiles = fileRepository.findRecentFiles(currentUser, pageable);

        return recentFiles.getContent().stream()
                .map(this::mapFileToFileResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Timed("file.favorites.paginated")
    public PageResponse<FileResponse> getFavoriteFiles(Pageable pageable) {
        User currentUser = getCurrentUser();

        // Get favorite files with pagination
        Page<File> favoriteFilesPage = fileRepository.findFavorites(currentUser, pageable);

        // Map to response DTOs
        Page<FileResponse> fileResponsePage = favoriteFilesPage.map(this::mapFileToFileResponse);

        // Create page response
        return PageResponse.fromPage(fileResponsePage);
    }

    @Override
    @Deprecated
    @Timed("file.favorites")
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
    @Timed("file.search.paginated")
    public PageResponse<FileResponse> searchFiles(String keyword, Pageable pageable) {
        User currentUser = getCurrentUser();

        if (keyword == null || keyword.trim().isEmpty()) {
            // Return empty page if keyword is empty
            return PageResponse.fromPage(Page.empty(pageable));
        }

        // Search files by name with pagination
        Page<File> searchResultsPage = fileRepository.searchByFilename(currentUser, keyword, pageable);

        // Map to response DTOs
        Page<FileResponse> fileResponsePage = searchResultsPage.map(this::mapFileToFileResponse);

        // Create page response
        return PageResponse.fromPage(fileResponsePage);
    }

    @Override
    @Deprecated
    @Timed("file.search")
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

    /**
     * Search files by content (if content indexing is enabled)
     */
    public PageResponse<FileResponse> searchFilesByContent(String keyword, Pageable pageable) {
        // Implementation would depend on your content indexing solution (e.g., Elasticsearch)
        // For now, just search by filename
        return searchFiles(keyword, pageable);
    }

    @Override
    @Transactional
    @Timed("file.cleanup")
    public int cleanupDeletedFiles(int daysInTrash) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysInTrash);
        List<File> filesToDelete = fileRepository.findByIsDeletedTrueAndDeletedAtBefore(cutoffDate);

        int count = 0;
        for (File file : filesToDelete) {
            try {
                // Keep track of user and file size for quota update
                Long userId = file.getUser().getId();
                Long fileSize = file.getFileSize();
                String storagePath = file.getStoragePath();
                String thumbnailPath = file.getThumbnailPath();

                // Delete from database
                fileRepository.delete(file);

                // Update user's storage quota
                quotaService.releaseStorage(userId, fileSize);

                // Only delete from storage if no other files are using the same storage path
                if (!fileRepository.existsByStoragePath(storagePath)) {
                    storageService.delete(storagePath);
                }

                // Delete thumbnail if exists
                if (thumbnailPath != null && !thumbnailPath.isEmpty()) {
                    storageService.delete(thumbnailPath);
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

    /**
     * Generate thumbnail asynchronously using the ThumbnailService
     */
    @Async
    protected void generateThumbnailAsync(Long fileId) {
        try {
            thumbnailService.generateThumbnailForFile(fileId);
        } catch (Exception e) {
            log.error("Error generating thumbnail for file: {}", fileId, e);
        }
    }

    /**
     * Delete storage chunks
     */
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

    /**
     * Generate a copy name for duplicate files
     */
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

    /**
     * Get the current authenticated user
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userPrincipal.getId()));
    }

    /**
     * Log file activity
     */
    private void logFileActivity(Long userId, String activity, Long fileId, String description) {
        activityService.logActivity(
                userId,
                activity,
                Constants.ITEM_TYPE_FILE,
                fileId,
                description
        );
    }

    /**
     * Send WebSocket notification for file upload
     */
    private void notifyFileUpload(FileResponse fileResponse) {
        webSocketService.notifyFileUpload(fileResponse);
    }

    /**
     * Send WebSocket notification for file update
     */
    private void notifyFileUpdate(FileResponse fileResponse) {
        webSocketService.notifyFileUpdate(fileResponse);
    }

    /**
     * Send WebSocket notification for file deletion
     */
    private void notifyFileDelete(Long fileId, Long ownerId, String fileName, Long parentFolderId) {
        webSocketService.notifyFileDelete(fileId, ownerId, fileName, parentFolderId);
    }

    /**
     * Send WebSocket notification for file move
     */
    private void notifyFileMove(FileResponse fileResponse, Long oldFolderId) {
        webSocketService.notifyFileMove(fileResponse, oldFolderId);
    }

    /**
     * Map File entity to FileResponse DTO
     */
    private FileResponse mapFileToFileResponse(File file) {
        String downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/files/download/")
                .path(file.getId().toString())
                .toUriString();

        // Generate thumbnail URL if applicable
        String thumbnailUrl = null;
        if (file.getThumbnailPath() != null && !file.getThumbnailPath().isEmpty()) {
            thumbnailUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/v1/files/thumbnail/")
                    .path(file.getId().toString())
                    .toUriString();
        } else if (file.getFileType() != null &&
                (file.getFileType().equals("image") || file.getFileType().equals("video"))) {
            thumbnailUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/v1/files/thumbnail/")
                    .path(file.getId().toString())
                    .toUriString();
        }

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
                .isShared(file.isShared())
                .downloadUrl(downloadUrl)
                .thumbnailUrl(thumbnailUrl)
                .createdAt(file.getCreatedAt())
                .updatedAt(file.getUpdatedAt())
                .lastAccessed(file.getLastAccessed())
                .owner(file.getUser().getUsername())
                .ownerId(file.getUser().getId())
                .build();
    }
}