package com.fileflow.controller;

import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.file.FileResponse;
import com.fileflow.model.Tag;
import com.fileflow.service.tag.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
@Tag(name = "Tag Management", description = "Tag management API")
@SecurityRequirement(name = "bearerAuth")
public class TagController {

    private final TagService tagService;

    @PostMapping
    @Operation(summary = "Create a new tag")
    public ResponseEntity<Tag> createTag(
            @RequestParam @NotBlank String name,
            @RequestParam(required = false) String color) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tagService.createTag(name, color));
    }

    @GetMapping
    @Operation(summary = "Get all tags for current user")
    public ResponseEntity<List<Tag>> getUserTags() {
        return ResponseEntity.ok(tagService.getUserTags());
    }

    @PutMapping("/{tagId}")
    @Operation(summary = "Update tag")
    public ResponseEntity<Tag> updateTag(
            @PathVariable Long tagId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String color) {
        return ResponseEntity.ok(tagService.updateTag(tagId, name, color));
    }

    @DeleteMapping("/{tagId}")
    @Operation(summary = "Delete tag")
    public ResponseEntity<ApiResponse> deleteTag(@PathVariable Long tagId) {
        return ResponseEntity.ok(tagService.deleteTag(tagId));
    }

    @PostMapping("/{tagId}/files/{fileId}")
    @Operation(summary = "Add tag to file")
    public ResponseEntity<ApiResponse> addTagToFile(
            @PathVariable Long tagId,
            @PathVariable Long fileId) {
        return ResponseEntity.ok(tagService.addTagToFile(tagId, fileId));
    }

    @DeleteMapping("/{tagId}/files/{fileId}")
    @Operation(summary = "Remove tag from file")
    public ResponseEntity<ApiResponse> removeTagFromFile(
            @PathVariable Long tagId,
            @PathVariable Long fileId) {
        return ResponseEntity.ok(tagService.removeTagFromFile(tagId, fileId));
    }

    @GetMapping("/{tagId}/files")
    @Operation(summary = "Get files with tag")
    public ResponseEntity<List<FileResponse>> getFilesWithTag(@PathVariable Long tagId) {
        return ResponseEntity.ok(tagService.getFilesWithTag(tagId));
    }
}
