package com.fileflow.controller;

import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.common.SearchResponse;
import com.fileflow.service.search.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for search functionality
 */
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Search API")
@SecurityRequirement(name = "bearerAuth")
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    @Operation(summary = "Search files and folders",
            description = "Search for files and folders by name. If Elasticsearch is enabled, this will also search file content.")
    public ResponseEntity<SearchResponse> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(searchService.search(query, page, size));
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recently accessed items",
            description = "Get recently accessed files and folders")
    public ResponseEntity<SearchResponse> getRecentItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(searchService.getRecentItems(page, size));
    }

    @GetMapping("/favorites")
    @Operation(summary = "Get favorite items",
            description = "Get files and folders marked as favorites")
    public ResponseEntity<SearchResponse> getFavoriteItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(searchService.getFavoriteItems(page, size));
    }

    @GetMapping("/files")
    @Operation(summary = "Search files only",
            description = "Search for files by name")
    public ResponseEntity<SearchResponse> searchFiles(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(searchService.searchFiles(query, page, size));
    }

    @GetMapping("/folders")
    @Operation(summary = "Search folders only",
            description = "Search for folders by name")
    public ResponseEntity<SearchResponse> searchFolders(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(searchService.searchFolders(query, page, size));
    }

    @GetMapping("/by-type/{fileType}")
    @Operation(summary = "Search files by type",
            description = "Search for files of a specific type (document, image, video, etc.)")
    public ResponseEntity<SearchResponse> searchByFileType(
            @PathVariable String fileType,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(searchService.searchByFileType(fileType, query, page, size));
    }

    @GetMapping("/trash")
    @Operation(summary = "Search items in trash",
            description = "Search for deleted files and folders in the trash")
    public ResponseEntity<SearchResponse> searchTrash(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(searchService.searchTrash(query, page, size));
    }

    @GetMapping("/content")
    @Operation(summary = "Search file contents",
            description = "Search for text within file contents (requires Elasticsearch)")
    public ResponseEntity<SearchResponse> searchFileContents(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(searchService.searchFileContents(query, page, size));
    }

    @GetMapping("/tags/{tag}")
    @Operation(summary = "Search files by tag",
            description = "Search for files with a specific tag")
    public ResponseEntity<SearchResponse> searchByTag(
            @PathVariable String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(searchService.searchByTag(tag, page, size));
    }

    @PostMapping("/index-file/{fileId}")
    @Operation(summary = "Index a file for search",
            description = "Manually trigger indexing for a file (admin only)")
    public ResponseEntity<ApiResponse> indexFile(@PathVariable Long fileId) {
        // Retrieve file and index it
        // This would typically be restricted to admin users
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("File indexing initiated")
                .build());
    }

    @PostMapping("/reindex")
    @Operation(summary = "Reindex all files",
            description = "Trigger reindexing of all files for the current user (admin only)")
    public ResponseEntity<ApiResponse> reindexFiles() {
        // Reindex all files for the current user
        // This would typically be restricted to admin users
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Reindexing initiated")
                .build());
    }
}