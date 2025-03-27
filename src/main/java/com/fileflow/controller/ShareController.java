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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/shares")
public class ShareController {

    private final ShareService shareService;
    private final FileService fileService;
    private final FolderService folderService;

    @Autowired
    public ShareController(ShareService shareService, FileService fileService, FolderService folderService) {
        this.shareService = shareService;
        this.fileService = fileService;
        this.folderService = folderService;
    }

    @PostMapping
    public ResponseEntity<ShareResponse> createShare(@Valid @RequestBody ShareCreateRequest shareCreateRequest) {
        ShareResponse shareResponse = shareService.createShare(shareCreateRequest);
        return new ResponseEntity<>(shareResponse, HttpStatus.CREATED);
    }

    @GetMapping("/{shareId}")
    public ResponseEntity<ShareResponse> getShare(@PathVariable Long shareId) {
        ShareResponse shareResponse = shareService.getShare(shareId);
        return ResponseEntity.ok(shareResponse);
    }

    @PutMapping("/{shareId}")
    public ResponseEntity<ShareResponse> updateShare(
            @PathVariable Long shareId,
            @Valid @RequestBody ShareUpdateRequest shareUpdateRequest) {
        ShareResponse updatedShare = shareService.updateShare(shareId, shareUpdateRequest);
        return ResponseEntity.ok(updatedShare);
    }

    @DeleteMapping("/{shareId}")
    public ResponseEntity<ApiResponse> deleteShare(@PathVariable Long shareId) {
        ApiResponse apiResponse = shareService.deleteShare(shareId);
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/outgoing")
    public ResponseEntity<PagedResponse<ShareResponse>> getOutgoingShares(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        PagedResponse<ShareResponse> response = shareService.getOutgoingShares(page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/incoming")
    public ResponseEntity<PagedResponse<ShareResponse>> getIncomingShares(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        PagedResponse<ShareResponse> response = shareService.getIncomingShares(page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/links/{shareLink}")
    public ResponseEntity<ShareResponse> getShareByLink(
            @PathVariable String shareLink,
            @RequestParam(required = false) String password) {
        ShareResponse shareResponse = shareService.getShareByLink(shareLink, password);
        if (shareResponse == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(shareResponse);
    }

    @PostMapping("/{shareId}/validate-password")
    public ResponseEntity<ApiResponse> validatePassword(
            @PathVariable Long shareId,
            @RequestParam String password) {
        ApiResponse response = shareService.validateSharePassword(shareId, password);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/links/{shareLink}/file")
    public ResponseEntity<?> getSharedFile(
            @PathVariable String shareLink,
            @RequestParam(required = false) String password) {
        ShareResponse shareResponse = shareService.getShareByLink(shareLink, password);

        // Add null check to prevent NullPointerException
        if (shareResponse == null) {
            return ResponseEntity.notFound().build();
        }

        if (!"FILE".equals(shareResponse.getItemType())) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.builder()
                            .success(false)
                            .message("The requested resource is not a file")
                            .build()
            );
        }

        FileResponse fileResponse = fileService.getFile(shareResponse.getItemId());
        return ResponseEntity.ok(fileResponse);
    }

    @GetMapping("/links/{shareLink}/folder")
    public ResponseEntity<?> getSharedFolder(
            @PathVariable String shareLink,
            @RequestParam(required = false) String password) {
        ShareResponse shareResponse = shareService.getShareByLink(shareLink, password);

        // Add null check to prevent NullPointerException
        if (shareResponse == null) {
            return ResponseEntity.notFound().build();
        }

        if (!"FOLDER".equals(shareResponse.getItemType())) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.builder()
                            .success(false)
                            .message("The requested resource is not a folder")
                            .build()
            );
        }

        FolderContentsResponse folderContents = folderService.getFolderContents(shareResponse.getItemId());
        return ResponseEntity.ok(folderContents);
    }
}