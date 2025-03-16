package com.fileflow.controller;

import com.fileflow.dto.request.folder.FolderCreateRequest;
import com.fileflow.dto.request.folder.FolderUpdateRequest;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.folder.FolderContentsResponse;
import com.fileflow.dto.response.folder.FolderResponse;
import com.fileflow.service.folder.FolderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/folders")
@RequiredArgsConstructor
@Tag(name = "Folder Management", description = "Folder management API")
@SecurityRequirement(name = "bearerAuth")
public class FolderController {

    private final FolderService folderService;

    @PostMapping
    @Operation(summary = "Create a new folder")
    public ResponseEntity<FolderResponse> createFolder(@Valid @RequestBody FolderCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(folderService.createFolder(request));
    }

    @GetMapping("/{folderId}")
    @Operation(summary = "Get folder details")
    public ResponseEntity<FolderResponse> getFolder(@PathVariable Long folderId) {
        return ResponseEntity.ok(folderService.getFolder(folderId));
    }

    @GetMapping("/{folderId}/contents")
    @Operation(summary = "Get folder contents")
    public ResponseEntity<FolderContentsResponse> getFolderContents(@PathVariable Long folderId) {
        return ResponseEntity.ok(folderService.getFolderContents(folderId));
    }

    @GetMapping("/root/contents")
    @Operation(summary = "Get root folder contents")
    public ResponseEntity<FolderContentsResponse> getRootFolderContents() {
        return ResponseEntity.ok(folderService.getFolderContents(null));
    }

    @PutMapping("/{folderId}")
    @Operation(summary = "Update folder")
    public ResponseEntity<FolderResponse> updateFolder(
            @PathVariable Long folderId,
            @Valid @RequestBody FolderUpdateRequest request) {
        return ResponseEntity.ok(folderService.updateFolder(folderId, request));
    }

    @DeleteMapping("/{folderId}")
    @Operation(summary = "Move folder to trash")
    public ResponseEntity<ApiResponse> deleteFolder(@PathVariable Long folderId) {
        return ResponseEntity.ok(folderService.deleteFolder(folderId));
    }

    @PostMapping("/{folderId}/restore")
    @Operation(summary = "Restore folder from trash")
    public ResponseEntity<ApiResponse> restoreFolder(@PathVariable Long folderId) {
        return ResponseEntity.ok(folderService.restoreFolder(folderId));
    }

    @DeleteMapping("/{folderId}/permanent")
    @Operation(summary = "Permanently delete folder")
    public ResponseEntity<ApiResponse> permanentDeleteFolder(@PathVariable Long folderId) {
        return ResponseEntity.ok(folderService.permanentDeleteFolder(folderId));
    }

    @PostMapping("/{folderId}/move")
    @Operation(summary = "Move folder to another location")
    public ResponseEntity<FolderResponse> moveFolder(
            @PathVariable Long folderId,
            @RequestParam Long destinationFolderId) {
        return ResponseEntity.ok(folderService.moveFolder(folderId, destinationFolderId));
    }

    @PostMapping("/{folderId}/copy")
    @Operation(summary = "Copy folder to another location")
    public ResponseEntity<FolderResponse> copyFolder(
            @PathVariable Long folderId,
            @RequestParam Long destinationFolderId) {
        return ResponseEntity.ok(folderService.copyFolder(folderId, destinationFolderId));
    }

    @GetMapping("/root")
    @Operation(summary = "Get root folders")
    public ResponseEntity<List<FolderResponse>> getRootFolders() {
        return ResponseEntity.ok(folderService.getRootFolders());
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent folders")
    public ResponseEntity<List<FolderResponse>> getRecentFolders(
            @RequestParam(defaultValue = "8") int limit) {
        return ResponseEntity.ok(folderService.getRecentFolders(limit));
    }

    @GetMapping("/{folderId}/path")
    @Operation(summary = "Get folder path (breadcrumbs)")
    public ResponseEntity<List<FolderResponse>> getFolderPath(@PathVariable Long folderId) {
        return ResponseEntity.ok(folderService.getFolderPath(folderId));
    }
}
