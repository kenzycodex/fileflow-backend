package com.fileflow.controller;

import com.fileflow.dto.request.file.FileUpdateRequest;
import com.fileflow.dto.request.file.FileUploadRequest;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.file.FileResponse;
import com.fileflow.dto.response.file.FileUploadResponse;
import com.fileflow.service.file.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Tag(name = "File Management", description = "File management API")
@SecurityRequirement(name = "bearerAuth")
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    @Operation(summary = "Upload a file")
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
    public ResponseEntity<List<FileResponse>> getFilesInFolder(@PathVariable Long folderId) {
        return ResponseEntity.ok(fileService.getFilesInFolder(folderId));
    }

    @GetMapping("/root")
    @Operation(summary = "Get files in root folder")
    public ResponseEntity<List<FileResponse>> getFilesInRoot() {
        return ResponseEntity.ok(fileService.getFilesInFolder(null));
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent files")
    public ResponseEntity<List<FileResponse>> getRecentFiles(
            @RequestParam(defaultValue = "10") int limit) {

        return ResponseEntity.ok(fileService.getRecentFiles(limit));
    }

    @GetMapping("/favorites")
    @Operation(summary = "Get favorite files")
    public ResponseEntity<List<FileResponse>> getFavoriteFiles() {
        return ResponseEntity.ok(fileService.getFavoriteFiles());
    }

    @GetMapping("/search")
    @Operation(summary = "Search files by name")
    public ResponseEntity<List<FileResponse>> searchFiles(
            @RequestParam String keyword) {

        return ResponseEntity.ok(fileService.searchFiles(keyword));
    }
}