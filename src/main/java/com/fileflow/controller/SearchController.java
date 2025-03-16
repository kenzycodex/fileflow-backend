package com.fileflow.controller;

import com.fileflow.dto.response.common.SearchResponse;
import com.fileflow.service.search.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Search API")
@SecurityRequirement(name = "bearerAuth")
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    @Operation(summary = "Search files and folders")
    public ResponseEntity<SearchResponse> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(searchService.search(query, page, size));
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recently accessed items")
    public ResponseEntity<SearchResponse> getRecentItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(searchService.getRecentItems(page, size));
    }

    @GetMapping("/favorites")
    @Operation(summary = "Get favorite items")
    public ResponseEntity<SearchResponse> getFavoriteItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(searchService.getFavoriteItems(page, size));
    }

    @GetMapping("/files")
    @Operation(summary = "Search files only")
    public ResponseEntity<SearchResponse> searchFiles(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(searchService.searchFiles(query, page, size));
    }

    @GetMapping("/folders")
    @Operation(summary = "Search folders only")
    public ResponseEntity<SearchResponse> searchFolders(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(searchService.searchFolders(query, page, size));
    }

    @GetMapping("/by-type/{fileType}")
    @Operation(summary = "Search files by type")
    public ResponseEntity<SearchResponse> searchByFileType(
            @PathVariable String fileType,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(searchService.searchByFileType(fileType, query, page, size));
    }

    @GetMapping("/trash")
    @Operation(summary = "Search items in trash")
    public ResponseEntity<SearchResponse> searchTrash(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(searchService.searchTrash(query, page, size));
    }
}