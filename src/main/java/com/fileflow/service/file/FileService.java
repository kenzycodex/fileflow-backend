package com.fileflow.service.file;

import com.fileflow.dto.request.file.ChunkUploadRequest;
import com.fileflow.dto.request.file.FileUpdateRequest;
import com.fileflow.dto.request.file.FileUploadRequest;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.common.PageResponse;
import com.fileflow.dto.response.file.FileResponse;
import com.fileflow.dto.response.file.FileUploadResponse;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;

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
     * Get files in folder with pagination
     *
     * @param folderId Folder ID (null for root)
     * @param pageable Pagination information
     * @return Paginated list of files
     */
    PageResponse<FileResponse> getFilesInFolder(Long folderId, Pageable pageable);

    /**
     * Get files in folder (no pagination - for backwards compatibility)
     *
     * @param folderId Folder ID (null for root)
     * @return List of files
     * @deprecated Use {@link #getFilesInFolder(Long, Pageable)} instead
     */
    @Deprecated
    List<FileResponse> getFilesInFolder(Long folderId);

    /**
     * Get recent files with pagination
     *
     * @param pageable Pagination information
     * @return Paginated list of recent files
     */
    PageResponse<FileResponse> getRecentFiles(Pageable pageable);

    /**
     * Get recent files with limit (for backwards compatibility)
     *
     * @param limit Maximum number of files to return
     * @return List of recent files
     * @deprecated Use {@link #getRecentFiles(Pageable)} instead
     */
    @Deprecated
    List<FileResponse> getRecentFiles(int limit);

    /**
     * Get favorite files with pagination
     *
     * @param pageable Pagination information
     * @return Paginated list of favorite files
     */
    PageResponse<FileResponse> getFavoriteFiles(Pageable pageable);

    /**
     * Get favorite files (no pagination - for backwards compatibility)
     *
     * @return List of favorite files
     * @deprecated Use {@link #getFavoriteFiles(Pageable)} instead
     */
    @Deprecated
    List<FileResponse> getFavoriteFiles();

    /**
     * Search files by name with pagination
     *
     * @param keyword Search keyword
     * @param pageable Pagination information
     * @return Paginated list of matching files
     */
    PageResponse<FileResponse> searchFiles(String keyword, Pageable pageable);

    /**
     * Search files by name (no pagination - for backwards compatibility)
     *
     * @param keyword Search keyword
     * @return List of matching files
     * @deprecated Use {@link #searchFiles(String, Pageable)} instead
     */
    @Deprecated
    List<FileResponse> searchFiles(String keyword);

    /**
     * Clean up deleted files from storage
     */
    int cleanupDeletedFiles(int daysInTrash);
}