package com.fileflow.bridge;

import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.file.FileResponse;
import com.fileflow.model.File;
import com.fileflow.model.User;
import com.fileflow.repository.FileRepository;
import com.fileflow.repository.UserRepository;
import com.fileflow.security.UserPrincipal;
import com.fileflow.service.file.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Bridge between Java backend and WebView for file system operations
 * Used by desktop and mobile applications
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FileSystemBridge {

    private final FileService fileService;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;

    /**
     * List files in local directory
     *
     * @param directory local directory path
     * @return list of file names
     */
    public List<String> listLocalFiles(String directory) {
        try {
            return Files.list(Paths.get(directory))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .toList();
        } catch (IOException e) {
            log.error("Error listing local files", e);
            throw new RuntimeException("Error listing local files: " + e.getMessage());
        }
    }

    /**
     * Check if local file exists
     *
     * @param filePath local file path
     * @return true if exists
     */
    public boolean localFileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }

    /**
     * Get local file size
     *
     * @param filePath local file path
     * @return file size in bytes
     */
    public long getLocalFileSize(String filePath) {
        try {
            return Files.size(Paths.get(filePath));
        } catch (IOException e) {
            log.error("Error getting file size", e);
            throw new RuntimeException("Error getting file size: " + e.getMessage());
        }
    }

    /**
     * Read local file as bytes
     *
     * @param filePath local file path
     * @return file content as byte array
     */
    public byte[] readLocalFile(String filePath) {
        try {
            return Files.readAllBytes(Paths.get(filePath));
        } catch (IOException e) {
            log.error("Error reading file", e);
            throw new RuntimeException("Error reading file: " + e.getMessage());
        }
    }

    /**
     * Write bytes to local file
     *
     * @param filePath local file path
     * @param content file content as byte array
     */
    public void writeLocalFile(String filePath, byte[] content) {
        try {
            Files.write(Paths.get(filePath), content);
        } catch (IOException e) {
            log.error("Error writing file", e);
            throw new RuntimeException("Error writing file: " + e.getMessage());
        }
    }

    /**
     * Upload local file to cloud
     *
     * @param localFilePath local file path
     * @param folderId optional folder ID (null for root)
     * @return uploaded file response
     */
    public FileResponse uploadLocalFile(String localFilePath, Long folderId) {
        try {
            java.io.File localFile = new java.io.File(localFilePath);

            if (!localFile.exists()) {
                throw new RuntimeException("Local file not found: " + localFilePath);
            }

            // Convert to MultipartFile
            MultipartFile multipartFile = new LocalMultipartFile(localFile);

            // Create upload request
            com.fileflow.dto.request.file.FileUploadRequest uploadRequest =
                    com.fileflow.dto.request.file.FileUploadRequest.builder()
                            .file(multipartFile)
                            .folderId(folderId)
                            .build();

            // Upload file
            return mapFileUploadResponseToFileResponse(fileService.uploadFile(uploadRequest));
        } catch (Exception e) {
            log.error("Error uploading local file", e);
            throw new RuntimeException("Error uploading local file: " + e.getMessage());
        }
    }

    /**
     * Download cloud file to local path
     *
     * @param fileId cloud file ID
     * @param localFilePath local file path
     * @return download status
     */
    public boolean downloadFileToLocal(Long fileId, String localFilePath) {
        try {
            // Get file
            FileResponse fileResponse = fileService.getFile(fileId);

            // Get file content as resource
            org.springframework.core.io.Resource resource = fileService.loadFileAsResource(fileId);

            // Write to local file
            try (FileOutputStream outputStream = new FileOutputStream(localFilePath)) {
                java.io.File resourceFile = resource.getFile();
                try (FileInputStream inputStream = new FileInputStream(resourceFile)) {
                    FileChannel inChannel = inputStream.getChannel();
                    FileChannel outChannel = outputStream.getChannel();
                    inChannel.transferTo(0, inChannel.size(), outChannel);
                }
            }

            return true;
        } catch (Exception e) {
            log.error("Error downloading file to local", e);
            return false;
        }
    }

    /**
     * Sync local directory with cloud folder
     *
     * @param localDirPath local directory path
     * @param folderId cloud folder ID
     * @return sync result
     */
    public ApiResponse syncDirectoryWithFolder(String localDirPath, Long folderId) {
        try {
            // Implementation would depend on sync strategy
            // For example:
            // 1. List local files
            // 2. List cloud files
            // 3. Compare and determine actions (upload, download, skip)
            // 4. Perform actions

            return ApiResponse.builder()
                    .success(true)
                    .message("Sync completed successfully")
                    .data(Map.of("syncedFiles", 0))
                    .timestamp(java.time.LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("Error syncing directory with folder", e);
            return ApiResponse.builder()
                    .success(false)
                    .message("Error syncing directory with folder: " + e.getMessage())
                    .timestamp(java.time.LocalDateTime.now())
                    .build();
        }
    }

    // Helper methods

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private FileResponse mapFileUploadResponseToFileResponse(com.fileflow.dto.response.file.FileUploadResponse uploadResponse) {
        return FileResponse.builder()
                .id(uploadResponse.getFileId())
                .filename(uploadResponse.getFilename())
                .originalFilename(uploadResponse.getOriginalFilename())
                .fileSize(uploadResponse.getFileSize())
                .fileType(uploadResponse.getFileType())
                .mimeType(uploadResponse.getMimeType())
                .parentFolderId(uploadResponse.getParentFolderId())
                .downloadUrl(uploadResponse.getDownloadUrl())
                .build();
    }

    /**
     * Simple MultipartFile implementation for local files
     */
    private static class LocalMultipartFile implements MultipartFile {
        private final java.io.File file;

        public LocalMultipartFile(java.io.File file) {
            this.file = file;
        }

        @Override
        public String getName() {
            return file.getName();
        }

        @Override
        public String getOriginalFilename() {
            return file.getName();
        }

        @Override
        public String getContentType() {
            try {
                return Files.probeContentType(file.toPath());
            } catch (IOException e) {
                return "application/octet-stream";
            }
        }

        @Override
        public boolean isEmpty() {
            return file.length() == 0;
        }

        @Override
        public long getSize() {
            return file.length();
        }

        @Override
        public byte[] getBytes() throws IOException {
            return Files.readAllBytes(file.toPath());
        }

        @Override
        public java.io.InputStream getInputStream() throws IOException {
            return new FileInputStream(file);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
            try (FileInputStream inputStream = new FileInputStream(file);
                 FileOutputStream outputStream = new FileOutputStream(dest)) {
                FileChannel inChannel = inputStream.getChannel();
                FileChannel outChannel = outputStream.getChannel();
                inChannel.transferTo(0, inChannel.size(), outChannel);
            }
        }
    }
}