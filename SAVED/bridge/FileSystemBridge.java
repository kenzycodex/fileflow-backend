package com.fileflow.bridge;

import com.fileflow.bridge.core.BridgeInterface;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.file.FileResponse;
import com.fileflow.model.User;
import com.fileflow.repository.FileRepository;
import com.fileflow.repository.UserRepository;
import com.fileflow.security.UserPrincipal;
import com.fileflow.service.file.FileService;
import com.fileflow.util.JsonUtil;
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
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bridge between Java backend and WebView for file system operations
 * Used by desktop and mobile applications
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FileSystemBridge implements BridgeInterface {

    private final FileService fileService;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;

    @Override
    public String getBridgeName() {
        return "file";
    }

    /**
     * List files in local directory
     */
    public String listLocalFiles(String directory) {
        try {
            List<String> files = Files.list(Paths.get(directory))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .toList();

            Map<String, Object> result = new HashMap<>();
            result.put("files", files);
            result.put("directory", directory);
            result.put("count", files.size());
            result.put("timestamp", LocalDateTime.now().toString());

            return JsonUtil.toJson(result);
        } catch (IOException e) {
            log.error("Error listing local files", e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error listing local files: " + e.getMessage());
            error.put("timestamp", LocalDateTime.now().toString());

            return JsonUtil.toJson(error);
        }
    }

    /**
     * Check if local file exists
     */
    public String localFileExists(String filePath) {
        try {
            boolean exists = Files.exists(Paths.get(filePath));

            Map<String, Object> result = new HashMap<>();
            result.put("exists", exists);
            result.put("filePath", filePath);
            result.put("timestamp", LocalDateTime.now().toString());

            return JsonUtil.toJson(result);
        } catch (Exception e) {
            log.error("Error checking if file exists", e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error checking if file exists: " + e.getMessage());
            error.put("timestamp", LocalDateTime.now().toString());

            return JsonUtil.toJson(error);
        }
    }

    /**
     * Get local file info
     */
    public String getLocalFileInfo(String filePath) {
        try {
            Path path = Paths.get(filePath);
            boolean exists = Files.exists(path);

            if (!exists) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "File does not exist: " + filePath);
                error.put("timestamp", LocalDateTime.now().toString());

                return JsonUtil.toJson(error);
            }

            Map<String, Object> info = new HashMap<>();
            info.put("path", filePath);
            info.put("size", Files.size(path));
            info.put("lastModified", Files.getLastModifiedTime(path).toMillis());
            info.put("isDirectory", Files.isDirectory(path));
            info.put("isRegularFile", Files.isRegularFile(path));
            info.put("isSymbolicLink", Files.isSymbolicLink(path));
            info.put("isHidden", Files.isHidden(path));
            info.put("isReadable", Files.isReadable(path));
            info.put("isWritable", Files.isWritable(path));
            info.put("isExecutable", Files.isExecutable(path));
            info.put("fileName", path.getFileName().toString());
            info.put("contentType", Files.probeContentType(path));
            info.put("timestamp", LocalDateTime.now().toString());

            return JsonUtil.toJson(info);
        } catch (Exception e) {
            log.error("Error getting file info", e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error getting file info: " + e.getMessage());
            error.put("timestamp", LocalDateTime.now().toString());

            return JsonUtil.toJson(error);
        }
    }

    /**
     * Upload local file to cloud
     *
     * @param localFilePath local file path
     * @param folderId optional folder ID (null for root)
     * @return uploaded file response
     */
    public String uploadLocalFile(String localFilePath, String folderId) {
        try {
            java.io.File localFile = new java.io.File(localFilePath);

            if (!localFile.exists()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Local file not found: " + localFilePath);
                error.put("timestamp", LocalDateTime.now().toString());

                return JsonUtil.toJson(error);
            }

            // Convert to MultipartFile
            MultipartFile multipartFile = new LocalMultipartFile(localFile);

            // Create upload request
            Long folderIdLong = folderId != null && !folderId.isEmpty() ? Long.parseLong(folderId) : null;

            com.fileflow.dto.request.file.FileUploadRequest uploadRequest =
                    com.fileflow.dto.request.file.FileUploadRequest.builder()
                            .file(multipartFile)
                            .folderId(folderIdLong)
                            .build();

            // Upload file
            FileResponse response = mapFileUploadResponseToFileResponse(fileService.uploadFile(uploadRequest));

            return JsonUtil.toJson(response);
        } catch (Exception e) {
            log.error("Error uploading local file", e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error uploading local file: " + e.getMessage());
            error.put("timestamp", LocalDateTime.now().toString());

            return JsonUtil.toJson(error);
        }
    }

    /**
     * Download cloud file to local path
     */
    public String downloadFileToLocal(String fileId, String localFilePath) {
        try {
            Long fileIdLong = Long.parseLong(fileId);

            // Get file
            FileResponse fileResponse = fileService.getFile(fileIdLong);

            // Get file content as resource
            org.springframework.core.io.Resource resource = fileService.loadFileAsResource(fileIdLong);

            // Write to local file
            try (FileOutputStream outputStream = new FileOutputStream(localFilePath)) {
                java.io.File resourceFile = resource.getFile();
                try (FileInputStream inputStream = new FileInputStream(resourceFile)) {
                    FileChannel inChannel = inputStream.getChannel();
                    FileChannel outChannel = outputStream.getChannel();
                    inChannel.transferTo(0, inChannel.size(), outChannel);
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "File downloaded successfully");
            result.put("fileId", fileId);
            result.put("localFilePath", localFilePath);
            result.put("fileSize", Files.size(Paths.get(localFilePath)));
            result.put("timestamp", LocalDateTime.now().toString());

            return JsonUtil.toJson(result);
        } catch (Exception e) {
            log.error("Error downloading file to local", e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error downloading file to local: " + e.getMessage());
            error.put("timestamp", LocalDateTime.now().toString());

            return JsonUtil.toJson(error);
        }
    }

    /**
     * Create local directory
     */
    public String createLocalDirectory(String directoryPath) {
        try {
            Path path = Paths.get(directoryPath);
            Files.createDirectories(path);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Directory created successfully");
            result.put("directoryPath", directoryPath);
            result.put("timestamp", LocalDateTime.now().toString());

            return JsonUtil.toJson(result);
        } catch (Exception e) {
            log.error("Error creating local directory", e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error creating local directory: " + e.getMessage());
            error.put("timestamp", LocalDateTime.now().toString());

            return JsonUtil.toJson(error);
        }
    }

    /**
     * Check if this bridge is available in the current environment
     * @return true if available
     */
    public boolean isAvailable() {
        return true;
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