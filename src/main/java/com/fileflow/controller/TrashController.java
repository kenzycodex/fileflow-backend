package com.fileflow.controller;

import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.common.SearchResponse;
import com.fileflow.service.trash.TrashService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trash")
@RequiredArgsConstructor
@Tag(name = "Trash Management", description = "Trash management API")
@SecurityRequirement(name = "bearerAuth")
public class TrashController {

    private final TrashService trashService;

    @GetMapping
    @Operation(summary = "Get items in trash")
    public ResponseEntity<SearchResponse> getTrashItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(trashService.getTrashItems(page, size));
    }

    @DeleteMapping
    @Operation(summary = "Empty trash")
    public ResponseEntity<ApiResponse> emptyTrash() {
        return ResponseEntity.ok(trashService.emptyTrash());
    }

    @PostMapping("/restore-all")
    @Operation(summary = "Restore all items from trash")
    public ResponseEntity<ApiResponse> restoreAllFromTrash() {
        return ResponseEntity.ok(trashService.restoreAllFromTrash());
    }

    @GetMapping("/info")
    @Operation(summary = "Get trash information")
    public ResponseEntity<ApiResponse> getTrashInfo() {
        return ResponseEntity.ok(trashService.getTrashInfo());
    }
}