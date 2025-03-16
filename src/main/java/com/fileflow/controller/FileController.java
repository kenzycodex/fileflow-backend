package com.fileflow.controller;

import com.fileflow.dto.request.file.FileUploadRequest;
import com.fileflow.dto.request.file.FileUpdateRequest;
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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Tag(name = "File Management", description = "File management API")
@SecurityRequirement(name = "bearerAuth")
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    @Operation(summary = "Upload file")
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folderId", required = false) Long folderId,
            @RequestParam(value = "description", required = false) String description) throws IOException {

        FileUploadRequest uploadRequest = FileUploadRequest.builder()
                .file(file)
                .folderId(folderId)
                .description(description)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(fileService.uploadFile(uploadRequest));
    }

    @GetMapping("/{fileId}")
    @Operation(summary = "Get file metadata")
    public ResponseEntity<FileResponse> getFile(@PathVariable Long fileId) {
        return ResponseEntity.ok(fileService.getFile(fileId));
    }

    @GetMapping("/download/{fileId}")
    @Operation(summary = "Download file")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId, HttpServletRequest request) {
        FileResponse fileResponse = fileService.getFile(fileId);
        Resource resource = fileService.loadFileAsResource(fileId);

        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            // Fallback to application/octet-stream
        }

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileResponse.getFilename() + "\"")
                .body(resource);
    }

    @PutMapping("/{fileId}")
    @Operation(summary = "Update file metadata")
    public ResponseEntity<FileResponse> updateFile(
            @PathVariable Long fileId,
            @Valid @RequestBody FileUpdateRequest updateRequest) {
        return ResponseEntity.ok(fileService.updateFile(fileId, updateRequest));
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

    @PutMapping("/{fileId}/favorite")
    @Operation(summary = "Toggle favorite status")
    public ResponseEntity<FileResponse> toggleFavorite(@PathVariable Long fileId) {
        return ResponseEntity.ok(fileService.toggleFavorite(fileId));
    }

    @GetMapping("/folder/{folderId}")
    @Operation(summary = "Get files in folder")
    public ResponseEntity<List<FileResponse>> getFilesInFolder(@PathVariable Long folderId) {
        return ResponseEntity.ok(fileService.getFilesInFolder(folderId));
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
}
