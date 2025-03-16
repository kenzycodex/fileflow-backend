package com.fileflow.service.file;

import com.fileflow.dto.request.file.ChunkUploadRequest;
import com.fileflow.dto.request.file.FileUpdateRequest;
import com.fileflow.dto.request.file.FileUploadRequest;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.file.FileResponse;
import com.fileflow.dto.response.file.FileUploadResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface FileService {
    /**
     * Upload a file
     */
    FileUploadResponse uploadFile(FileUploadRequest uploadRequest) throws IOException;

    /**
     * Upload a file chunk
     */
    ApiResponse uploadChunk(ChunkUploadRequest chunkRequest) throws IOException;

    /**
     * Complete a chunked upload
     */
    FileUploadResponse completeChunkedUpload(String uploadId) throws IOException;

    /**
     * Get file metadata
     */
    FileResponse getFile(Long fileId);

    /**
     * Load file as resource for download
     */
    Resource loadFileAsResource(Long fileId);

    /**
     * Update file metadata
     */
    FileResponse updateFile(Long fileId, FileUpdateRequest updateRequest);

    /**
     * Move file to trash (soft delete)
     */
    ApiResponse deleteFile(Long fileId);

    /**
     * Restore file from trash
     */
    ApiResponse restoreFile(Long fileId);

    /**
     * Permanently delete file
     */
    ApiResponse permanentDeleteFile(Long fileId);

    /**
     * Move file to another folder
     */
    FileResponse moveFile(Long fileId, Long destinationFolderId);

    /**
     * Copy file to another folder
     */
    FileResponse copyFile(Long fileId, Long destinationFolderId);

    /**
     * Toggle favorite status
     */
    FileResponse toggleFavorite(Long fileId);

    /**
     * Get files in folder
     */
    List<FileResponse> getFilesInFolder(Long folderId);

    /**
     * Get recent files
     */
    List<FileResponse> getRecentFiles(int limit);

    /**
     * Get favorite files
     */
    List<FileResponse> getFavoriteFiles();

    /**
     * Search files by name
     */
    List<FileResponse> searchFiles(String keyword);

    /**
     * Clean up deleted files from storage
     */
    int cleanupDeletedFiles(int daysInTrash);
}