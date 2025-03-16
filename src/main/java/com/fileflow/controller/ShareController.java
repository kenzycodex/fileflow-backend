package com.fileflow.controller;

import com.fileflow.dto.request.share.ShareCreateRequest;
import com.fileflow.dto.request.share.ShareUpdateRequest;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.common.PagedResponse;
import com.fileflow.dto.response.file.FileResponse;
import com.fileflow.dto.response.folder.FolderContentsResponse;
import com.fileflow.dto.response.share.ShareResponse;
import com.fileflow.service.file.FileService;
import com.fileflow.service.folder.FolderService;
import com.fileflow.service.share.ShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/shares")
@RequiredArgsConstructor
@Tag(name = "Sharing", description = "Sharing API")
@SecurityRequirement(name = "bearerAuth")
public class ShareController {

    private final ShareService shareService;
    private final FileService fileService;
    private final FolderService folderService;

    @PostMapping
    @Operation(summary = "Create a new share")
    public ResponseEntity<ShareResponse> createShare(@Valid @RequestBody ShareCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shareService.createShare(request));
    }

    @GetMapping("/{shareId}")
    @Operation(summary = "Get share details")
    public ResponseEntity<ShareResponse> getShare(@PathVariable Long shareId) {
        return ResponseEntity.ok(shareService.getShare(shareId));
    }

    @PutMapping("/{shareId}")
    @Operation(summary = "Update share")
    public ResponseEntity<ShareResponse> updateShare(
            @PathVariable Long shareId,
            @Valid @RequestBody ShareUpdateRequest request) {
        return ResponseEntity.ok(shareService.updateShare(shareId, request));
    }

    @DeleteMapping("/{shareId}")
    @Operation(summary = "Delete share")
    public ResponseEntity<ApiResponse> deleteShare(@PathVariable Long shareId) {
        return ResponseEntity.ok(shareService.deleteShare(shareId));
    }

    @GetMapping("/outgoing")
    @Operation(summary = "Get outgoing shares (created by current user)")
    public ResponseEntity<PagedResponse<ShareResponse>> getOutgoingShares(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(shareService.getOutgoingShares(page, size));
    }

    @GetMapping("/incoming")
    @Operation(summary = "Get incoming shares (shared with current user)")
    public ResponseEntity<PagedResponse<ShareResponse>> getIncomingShares(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(shareService.getIncomingShares(page, size));
    }

    @GetMapping("/links/{shareLink}")
    @Operation(summary = "Access shared item by link")
    public ResponseEntity<ShareResponse> getShareByLink(
            @PathVariable String shareLink,
            @RequestParam(required = false) String password) {
        return ResponseEntity.ok(shareService.getShareByLink(shareLink, password));
    }

    @PostMapping("/{shareId}/validate-password")
    @Operation(summary = "Validate share password")
    public ResponseEntity<ApiResponse> validatePassword(
            @PathVariable Long shareId,
            @RequestParam String password) {
        return ResponseEntity.ok(shareService.validateSharePassword(shareId, password));
    }

    @GetMapping("/links/{shareLink}/file")
    @Operation(summary = "Get shared file content")
    public ResponseEntity<FileResponse> getSharedFile(
            @PathVariable String shareLink,
            @RequestParam(required = false) String password) {

        // First validate the share
        ShareResponse shareResponse = shareService.getShareByLink(shareLink, password);

        if (!"FILE".equals(shareResponse.getItemType())) {
            return ResponseEntity.badRequest().build();
        }

        // Get the file
        return ResponseEntity.ok(fileService.getFile(shareResponse.getItemId()));
    }

    @GetMapping("/links/{shareLink}/folder")
    @Operation(summary = "Get shared folder contents")
    public ResponseEntity<FolderContentsResponse> getSharedFolder(
            @PathVariable String shareLink,
            @RequestParam(required = false) String password) {

        // First validate the share
        ShareResponse shareResponse = shareService.getShareByLink(shareLink, password);

        if (!"FOLDER".equals(shareResponse.getItemType())) {
            return ResponseEntity.badRequest().build();
        }

        // Get the folder contents
        return ResponseEntity.ok(folderService.getFolderContents(shareResponse.getItemId()));
    }
}
