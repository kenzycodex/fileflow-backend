package com.fileflow.controller;

import com.fileflow.annotation.RateLimit;
import com.fileflow.dto.request.file.FileUpdateRequest;
import com.fileflow.dto.request.file.FileUploadRequest;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.common.PageResponse;
import com.fileflow.dto.response.file.FileResponse;
import com.fileflow.dto.response.file.FileUploadResponse;
import com.fileflow.service.file.FileService;
import com.fileflow.util.Constants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Tag(name = "File Management", description = "File management API")
@SecurityRequirement(name = "bearerAuth")
@RateLimit(type = RateLimit.LimitType.API, keyResolver = RateLimit.KeyResolver.USER)
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    @Operation(summary = "Upload a file")
    @RateLimit(type = RateLimit.LimitType.API)
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folderId", required = false) Long folderId,
            @RequestParam(value = "overwrite", required = false) Boolean overwrite) throws IOException {

        FileUploadRequest uploadRequest = FileUploadRequest.builder()
                .file(file)
                .folderId(folderId)
                .overwrite(overwrite)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(
                fileService.uploadFile(uploadRequest)
        );
    }

    @PostMapping("/upload/chunk")
    @Operation(summary = "Upload a file chunk (resumable upload)")
    @RateLimit(type = RateLimit.LimitType.API)
    public ResponseEntity<ApiResponse> uploadChunk(
            @RequestParam("chunk") MultipartFile chunk,
            @RequestParam("uploadId") String uploadId,
            @RequestParam("chunkNumber") Integer chunkNumber,
            @RequestParam("totalChunks") Integer totalChunks,
            @RequestParam("totalSize") Long totalSize,
            @RequestParam("originalFilename") String originalFilename,
            @RequestParam(value = "folderId", required = false) Long folderId) throws IOException {

        com.fileflow.dto.request.file.ChunkUploadRequest request = com.fileflow.dto.request.file.ChunkUploadRequest.builder()
                .chunk(chunk)
                .uploadId(uploadId)
                .chunkNumber(chunkNumber)
                .totalChunks(totalChunks)
                .totalSize(totalSize)
                .originalFilename(originalFilename)
                .folderId(folderId)
                .build();

        return ResponseEntity.ok(fileService.uploadChunk(request));
    }

    @PostMapping("/upload/chunk/complete")
    @Operation(summary = "Complete chunked upload")
    public ResponseEntity<FileUploadResponse> completeChunkedUpload(
            @RequestParam("uploadId") String uploadId) throws IOException {

        return ResponseEntity.status(HttpStatus.CREATED).body(
                fileService.completeChunkedUpload(uploadId)
        );
    }

    @PostMapping("/upload/prepare")
    @Operation(summary = "Prepare for chunked upload")
    public ResponseEntity<Map<String, String>> prepareChunkedUpload() {
        // Generate a unique upload ID
        String uploadId = UUID.randomUUID().toString();
        return ResponseEntity.ok(Map.of("uploadId", uploadId));
    }

    @GetMapping("/{fileId}")
    @Operation(summary = "Get file metadata")
    public ResponseEntity<FileResponse> getFile(@PathVariable Long fileId) {
        return ResponseEntity.ok(fileService.getFile(fileId));
    }

    @GetMapping("/download/{fileId}")
    @Operation(summary = "Download a file")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) {
        // Load file as resource
        Resource resource = fileService.loadFileAsResource(fileId);

        // Get file metadata
        FileResponse fileResponse = fileService.getFile(fileId);

        // Determine content type
        String contentType = fileResponse.getMimeType() != null ?
                fileResponse.getMimeType() : "application/octet-stream";

        // Set headers for download
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileResponse.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/preview/{fileId}")
    @Operation(summary = "Preview a file")
    public ResponseEntity<Resource> previewFile(@PathVariable Long fileId) {
        // Load file as resource
        Resource resource = fileService.loadFileAsResource(fileId);

        // Get file metadata
        FileResponse fileResponse = fileService.getFile(fileId);

        // Determine content type
        String contentType = fileResponse.getMimeType() != null ?
                fileResponse.getMimeType() : "application/octet-stream";

        // Set headers for inline viewing
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + fileResponse.getFilename() + "\"")
                .body(resource);
    }

    @PutMapping("/{fileId}")
    @Operation(summary = "Update file metadata")
    public ResponseEntity<FileResponse> updateFile(
            @PathVariable Long fileId,
            @Valid @RequestBody FileUpdateRequest request) {

        return ResponseEntity.ok(fileService.updateFile(fileId, request));
    }

    @DeleteMapping("/{fileId}")
    @Operation(summary = "Move file to trash")
    public ResponseEntity<ApiResponse> deleteFile(@PathVariable Long fileId) {
        return ResponseEntity.ok(fileService.deleteFile(fileId));
    }

    @PostMapping("/{fileId}/restore")
    @Operation(summary = "Restore file from trash")
    public ResponseEntity<ApiResponse> restoreFile(@PathVariable Long fileId) {
        return ResponseEntity.ok(fileService.restoreFile(fileId));
    }

    @DeleteMapping("/{fileId}/permanent")
    @Operation(summary = "Permanently delete file")
    public ResponseEntity<ApiResponse> permanentDeleteFile(@PathVariable Long fileId) {
        return ResponseEntity.ok(fileService.permanentDeleteFile(fileId));
    }

    @PostMapping("/{fileId}/move")
    @Operation(summary = "Move file to another folder")
    public ResponseEntity<FileResponse> moveFile(
            @PathVariable Long fileId,
            @RequestParam Long destinationFolderId) {

        return ResponseEntity.ok(fileService.moveFile(fileId, destinationFolderId));
    }

    @PostMapping("/{fileId}/copy")
    @Operation(summary = "Copy file to another folder")
    public ResponseEntity<FileResponse> copyFile(
            @PathVariable Long fileId,
            @RequestParam Long destinationFolderId) {

        return ResponseEntity.ok(fileService.copyFile(fileId, destinationFolderId));
    }

    @PostMapping("/{fileId}/favorite")
    @Operation(summary = "Toggle favorite status")
    public ResponseEntity<FileResponse> toggleFavorite(@PathVariable Long fileId) {
        return ResponseEntity.ok(fileService.toggleFavorite(fileId));
    }

    @GetMapping("/folder/{folderId}")
    @Operation(summary = "Get files in folder")
    public ResponseEntity<PageResponse<FileResponse>> getFilesInFolder(
            @PathVariable Long folderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "filename") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        // Validate and limit page size
        int validatedSize = Math.min(size, Constants.MAX_PAGE_SIZE);

        // Create pageable with sort direction
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, validatedSize, direction, sortBy);

        return ResponseEntity.ok(fileService.getFilesInFolder(folderId, pageable));
    }

    @GetMapping("/folder/{folderId}/all")
    @Operation(summary = "Get all files in folder (no pagination)")
    @Deprecated
    public ResponseEntity<List<FileResponse>> getAllFilesInFolder(@PathVariable Long folderId) {
        return ResponseEntity.ok(fileService.getFilesInFolder(folderId));
    }

    @GetMapping("/root")
    @Operation(summary = "Get files in root folder")
    public ResponseEntity<PageResponse<FileResponse>> getFilesInRoot(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "filename") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        // Validate and limit page size
        int validatedSize = Math.min(size, Constants.MAX_PAGE_SIZE);

        // Create pageable with sort direction
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, validatedSize, direction, sortBy);

        return ResponseEntity.ok(fileService.getFilesInFolder(null, pageable));
    }

    @GetMapping("/root/all")
    @Operation(summary = "Get all files in root folder (no pagination)")
    @Deprecated
    public ResponseEntity<List<FileResponse>> getAllFilesInRoot() {
        return ResponseEntity.ok(fileService.getFilesInFolder(null));
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent files")
    public ResponseEntity<PageResponse<FileResponse>> getRecentFiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // Validate and limit page size
        int validatedSize = Math.min(size, Constants.MAX_PAGE_SIZE);

        // Create pageable with default sort by last accessed time
        Pageable pageable = PageRequest.of(page, validatedSize, Sort.Direction.DESC, "lastAccessed");

        return ResponseEntity.ok(fileService.getRecentFiles(pageable));
    }

    @GetMapping("/recent/limit")
    @Operation(summary = "Get recent files with limit (no pagination)")
    @Deprecated
    public ResponseEntity<List<FileResponse>> getRecentFilesWithLimit(
            @RequestParam(defaultValue = "10") int limit) {

        // Validate and limit size
        int validatedLimit = Math.min(limit, Constants.MAX_PAGE_SIZE);

        return ResponseEntity.ok(fileService.getRecentFiles(validatedLimit));
    }

    @GetMapping("/favorites")
    @Operation(summary = "Get favorite files")
    public ResponseEntity<PageResponse<FileResponse>> getFavoriteFiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "lastAccessed") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        // Validate and limit page size
        int validatedSize = Math.min(size, Constants.MAX_PAGE_SIZE);

        // Create pageable with sort direction
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, validatedSize, direction, sortBy);

        return ResponseEntity.ok(fileService.getFavoriteFiles(pageable));
    }

    @GetMapping("/favorites/all")
    @Operation(summary = "Get all favorite files (no pagination)")
    @Deprecated
    public ResponseEntity<List<FileResponse>> getAllFavoriteFiles() {
        return ResponseEntity.ok(fileService.getFavoriteFiles());
    }

    @GetMapping("/search")
    @Operation(summary = "Search files by name")
    public ResponseEntity<PageResponse<FileResponse>> searchFiles(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "relevance") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        // Validate and limit page size
        int validatedSize = Math.min(size, Constants.MAX_PAGE_SIZE);

        // Create pageable with sort direction
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;

        // If sorting by relevance, use lastAccessed as a proxy (or could implement a custom sort)
        if ("relevance".equals(sortBy)) {
            sortBy = "lastAccessed";
        }

        Pageable pageable = PageRequest.of(page, validatedSize, direction, sortBy);

        return ResponseEntity.ok(fileService.searchFiles(keyword, pageable));
    }

    @GetMapping("/search/all")
    @Operation(summary = "Search all files (no pagination)")
    @Deprecated
    public ResponseEntity<List<FileResponse>> searchAllFiles(
            @RequestParam String keyword) {

        return ResponseEntity.ok(fileService.searchFiles(keyword));
    }
}