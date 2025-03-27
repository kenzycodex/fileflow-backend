package com.fileflow.service.search;

import com.fileflow.dto.response.common.SearchResponse;
import com.fileflow.dto.response.file.FileResponse;
import com.fileflow.dto.response.folder.FolderResponse;
import com.fileflow.exception.BadRequestException;
import com.fileflow.model.File;
import com.fileflow.model.Folder;
import com.fileflow.model.User;
import com.fileflow.model.search.FileSearchDocument;
import com.fileflow.repository.FileRepository;
import com.fileflow.repository.FolderRepository;
import com.fileflow.repository.UserRepository;
import com.fileflow.repository.FileTagRepository;
import com.fileflow.security.UserPrincipal;
import com.fileflow.service.storage.StorageServiceFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Default implementation of SearchService that uses database queries
 * This is enhanced with Elasticsearch capabilities when the elasticsearch profile is active
 */
@Service
@Primary
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService {

    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final FileTagRepository fileTagRepository;
    private final StorageServiceFactory storageServiceFactory;

    // Elasticsearch service - will be null if elasticsearch profile is not active
    @Autowired(required = false)
    private ElasticsearchSearchService elasticsearchSearchService;

    /**
     * Setter for elasticsearchSearchService - used for testing and when elasticsearch is available
     * @param elasticsearchSearchService The Elasticsearch search service
     */
    public void setElasticsearchSearchService(ElasticsearchSearchService elasticsearchSearchService) {
        this.elasticsearchSearchService = elasticsearchSearchService;
    }

    @Override
    public SearchResponse search(String query, int page, int size) {
        if (!StringUtils.hasText(query)) {
            throw new BadRequestException("Search query cannot be empty");
        }

        User currentUser = getCurrentUser();
        validatePageNumberAndSize(page, size);

        // If Elasticsearch is available and the query appears to be a content search
        // (contains spaces or is longer), use Elasticsearch for more comprehensive results
        if (elasticsearchSearchService != null && (query.contains(" ") || query.length() > 10)) {
            log.debug("Using Elasticsearch for search: {}", query);
            return elasticsearchSearchService.fullSearch(query, currentUser.getId(), page, size);
        }

        // Fallback to database search
        return performDatabaseSearch(query, currentUser, page, size);
    }

    private SearchResponse performDatabaseSearch(String query, User currentUser, int page, int size) {
        // Adjust size to split between files and folders
        int adjustedSize = size / 2;
        if (adjustedSize < 1) adjustedSize = 1;

        // Search files
        Pageable filePageable = PageRequest.of(page, adjustedSize, Sort.Direction.DESC, "lastAccessed");
        Page<File> files = fileRepository.searchByFilename(currentUser, query, filePageable);

        // Search folders
        Pageable folderPageable = PageRequest.of(page, adjustedSize, Sort.Direction.DESC, "lastAccessed");
        Page<Folder> folders = folderRepository.searchByFolderName(currentUser, query, folderPageable);

        // Map results
        List<FileResponse> fileResponses = files.getContent().stream()
                .map(this::mapFileToFileResponse)
                .collect(Collectors.toList());

        List<FolderResponse> folderResponses = folders.getContent().stream()
                .map(this::mapFolderToFolderResponse)
                .collect(Collectors.toList());

        // Calculate total elements and pages
        long totalElements = files.getTotalElements() + folders.getTotalElements();
        int totalPages = Math.max(files.getTotalPages(), folders.getTotalPages());

        return SearchResponse.builder()
                .files(fileResponses)
                .folders(folderResponses)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasMore(page < totalPages - 1)
                .query(query)
                .build();
    }

    @Override
    public SearchResponse getRecentItems(int page, int size) {
        User currentUser = getCurrentUser();
        validatePageNumberAndSize(page, size);

        // Adjust size to split between files and folders
        int adjustedSize = size / 2;
        if (adjustedSize < 1) adjustedSize = 1;

        // Get recent files
        Pageable filePageable = PageRequest.of(page, adjustedSize, Sort.Direction.DESC, "lastAccessed");
        Page<File> files = fileRepository.findRecentFiles(currentUser, filePageable);

        // Get recent folders
        Pageable folderPageable = PageRequest.of(page, adjustedSize, Sort.Direction.DESC, "lastAccessed");
        Page<Folder> folders = folderRepository.findRecentFolders(currentUser, folderPageable);

        // Map results
        List<FileResponse> fileResponses = files.getContent().stream()
                .map(this::mapFileToFileResponse)
                .collect(Collectors.toList());

        List<FolderResponse> folderResponses = folders.getContent().stream()
                .map(this::mapFolderToFolderResponse)
                .collect(Collectors.toList());

        // Calculate total elements and pages
        long totalElements = files.getTotalElements() + folders.getTotalElements();
        int totalPages = Math.max(files.getTotalPages(), folders.getTotalPages());

        return SearchResponse.builder()
                .files(fileResponses)
                .folders(folderResponses)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasMore(page < totalPages - 1)
                .build();
    }

    @Override
    public SearchResponse getFavoriteItems(int page, int size) {
        User currentUser = getCurrentUser();
        validatePageNumberAndSize(page, size);

        // Get favorite files
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "lastAccessed");
        Page<File> favoriteFiles = fileRepository.findFavorites(currentUser, pageable);

        // Get favorite folders (if implemented)
        List<FolderResponse> folderResponses = new ArrayList<>();
        if (folderRepository.findFavoriteFolders(currentUser) != null) {
            List<Folder> favoriteFolders = folderRepository.findFavoriteFolders(currentUser);
            folderResponses = favoriteFolders.stream()
                    .map(this::mapFolderToFolderResponse)
                    .collect(Collectors.toList());
        }

        // Map file results
        List<FileResponse> fileResponses = favoriteFiles.getContent().stream()
                .map(this::mapFileToFileResponse)
                .collect(Collectors.toList());

        // Calculate total elements and pages
        long totalElements = favoriteFiles.getTotalElements() + folderResponses.size();
        int totalPages = favoriteFiles.getTotalPages();

        return SearchResponse.builder()
                .files(fileResponses)
                .folders(folderResponses)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasMore(page < totalPages - 1)
                .build();
    }

    @Override
    public SearchResponse searchFiles(String query, int page, int size) {
        if (!StringUtils.hasText(query)) {
            throw new BadRequestException("Search query cannot be empty");
        }

        User currentUser = getCurrentUser();
        validatePageNumberAndSize(page, size);

        // If Elasticsearch is available, use it for more comprehensive file search
        if (elasticsearchSearchService != null) {
            log.debug("Using Elasticsearch for file search: {}", query);
            return elasticsearchSearchService.searchFiles(query, currentUser.getId(), page, size);
        }

        // Fallback to database search
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "lastAccessed");
        Page<File> files = fileRepository.searchByFilename(currentUser, query, pageable);

        // Map results
        List<FileResponse> fileResponses = files.getContent().stream()
                .map(this::mapFileToFileResponse)
                .collect(Collectors.toList());

        return SearchResponse.builder()
                .files(fileResponses)
                .folders(Collections.emptyList())
                .page(page)
                .size(size)
                .totalElements(files.getTotalElements())
                .totalPages(files.getTotalPages())
                .hasMore(page < files.getTotalPages() - 1)
                .query(query)
                .build();
    }

    @Override
    public SearchResponse searchFolders(String query, int page, int size) {
        if (!StringUtils.hasText(query)) {
            throw new BadRequestException("Search query cannot be empty");
        }

        User currentUser = getCurrentUser();
        validatePageNumberAndSize(page, size);

        // Search folders
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "lastAccessed");
        Page<Folder> folders = folderRepository.searchByFolderName(currentUser, query, pageable);

        // Map results
        List<FolderResponse> folderResponses = folders.getContent().stream()
                .map(this::mapFolderToFolderResponse)
                .collect(Collectors.toList());

        return SearchResponse.builder()
                .files(Collections.emptyList())
                .folders(folderResponses)
                .page(page)
                .size(size)
                .totalElements(folders.getTotalElements())
                .totalPages(folders.getTotalPages())
                .hasMore(page < folders.getTotalPages() - 1)
                .query(query)
                .build();
    }

    @Override
    public SearchResponse searchByFileType(String fileType, String query, int page, int size) {
        User currentUser = getCurrentUser();
        validatePageNumberAndSize(page, size);

        // If Elasticsearch is available, use it for file type search
        if (elasticsearchSearchService != null && StringUtils.hasText(query)) {
            log.debug("Using Elasticsearch for file type search: {} - {}", fileType, query);
            return elasticsearchSearchService.searchByFileType(fileType, query, currentUser.getId(), page, size);
        }

        // Fallback to database search
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "lastAccessed");
        Page<File> files;

        if (StringUtils.hasText(query)) {
            files = fileRepository.searchByFileTypeAndFilename(currentUser, fileType, query, pageable);
        } else {
            files = fileRepository.findByUserAndFileTypeAndIsDeletedFalse(currentUser, fileType, pageable);
        }

        // Map results
        List<FileResponse> fileResponses = files.getContent().stream()
                .map(this::mapFileToFileResponse)
                .collect(Collectors.toList());

        return SearchResponse.builder()
                .files(fileResponses)
                .folders(Collections.emptyList())
                .page(page)
                .size(size)
                .totalElements(files.getTotalElements())
                .totalPages(files.getTotalPages())
                .hasMore(page < files.getTotalPages() - 1)
                .query(query)
                .build();
    }

    @Override
    public SearchResponse searchTrash(String query, int page, int size) {
        User currentUser = getCurrentUser();
        validatePageNumberAndSize(page, size);

        // Adjust size to split between files and folders
        int adjustedSize = size / 2;
        if (adjustedSize < 1) adjustedSize = 1;

        // Search deleted files
        Pageable filePageable = PageRequest.of(page, adjustedSize, Sort.Direction.DESC, "deletedAt");
        Page<File> files;

        if (StringUtils.hasText(query)) {
            files = fileRepository.searchDeletedByFilename(currentUser, query, filePageable);
        } else {
            files = fileRepository.findByUserAndIsDeletedTrue(currentUser, filePageable);
        }

        // Search deleted folders
        Pageable folderPageable = PageRequest.of(page, adjustedSize, Sort.Direction.DESC, "deletedAt");
        Page<Folder> folders;

        if (StringUtils.hasText(query)) {
            folders = folderRepository.searchDeletedByFolderName(currentUser, query, folderPageable);
        } else {
            folders = folderRepository.findByUserAndIsDeletedTrue(currentUser, folderPageable);
        }

        // Map results
        List<FileResponse> fileResponses = files.getContent().stream()
                .map(this::mapFileToFileResponse)
                .collect(Collectors.toList());

        List<FolderResponse> folderResponses = folders.getContent().stream()
                .map(this::mapFolderToFolderResponse)
                .collect(Collectors.toList());

        // Calculate total elements and pages
        long totalElements = files.getTotalElements() + folders.getTotalElements();
        int totalPages = Math.max(files.getTotalPages(), folders.getTotalPages());

        return SearchResponse.builder()
                .files(fileResponses)
                .folders(folderResponses)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasMore(page < totalPages - 1)
                .query(query)
                .build();
    }

    @Override
    public SearchResponse searchFileContents(String query, int page, int size) {
        if (!StringUtils.hasText(query)) {
            throw new BadRequestException("Search query cannot be empty");
        }

        User currentUser = getCurrentUser();
        validatePageNumberAndSize(page, size);

        // Content search requires Elasticsearch
        if (elasticsearchSearchService != null) {
            log.debug("Using Elasticsearch for content search: {}", query);
            return elasticsearchSearchService.searchByContent(query, currentUser.getId(), page, size);
        }

        // If Elasticsearch is not available, return an empty result
        log.warn("Content search requested but Elasticsearch is not available");
        return SearchResponse.builder()
                .files(Collections.emptyList())
                .folders(Collections.emptyList())
                .page(page)
                .size(size)
                .totalElements(0)
                .totalPages(0)
                .hasMore(false)
                .query(query)
                .build();
    }

    @Override
    public SearchResponse searchByTag(String tag, int page, int size) {
        if (!StringUtils.hasText(tag)) {
            throw new BadRequestException("Tag cannot be empty");
        }

        User currentUser = getCurrentUser();
        validatePageNumberAndSize(page, size);

        // If Elasticsearch is available, use it for tag search
        if (elasticsearchSearchService != null) {
            log.debug("Using Elasticsearch for tag search: {}", tag);
            return elasticsearchSearchService.searchByTag(tag, currentUser.getId(), page, size);
        }

        // Manual lookup using JPA relationships
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "lastAccessed");

        // Get files with the specified tag
        List<File> taggedFiles = fileTagRepository.findFilesByTagNameAndUserId(tag, currentUser.getId(), pageable);

        // Map results
        List<FileResponse> fileResponses = taggedFiles.stream()
                .map(this::mapFileToFileResponse)
                .collect(Collectors.toList());

        return SearchResponse.builder()
                .files(fileResponses)
                .folders(Collections.emptyList())
                .page(page)
                .size(size)
                .totalElements(fileResponses.size())
                .totalPages(1)
                .hasMore(false)
                .query(tag)
                .build();
    }

    @Override
    public void indexFile(File file) {
        if (elasticsearchSearchService != null) {
            elasticsearchSearchService.indexFile(file);
        }
    }

    @Override
    public void removeFileFromIndex(Long fileId) {
        if (elasticsearchSearchService != null) {
            elasticsearchSearchService.removeFileIndex(fileId);
        }
    }

    // Helper methods

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void validatePageNumberAndSize(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("Page number cannot be less than zero.");
        }

        if (size < 1) {
            throw new BadRequestException("Page size must not be less than one.");
        }

        if (size > 100) {
            throw new BadRequestException("Page size must not be greater than 100.");
        }
    }

    private FileResponse mapFileToFileResponse(File file) {
        return FileResponse.builder()
                .id(file.getId())
                .filename(file.getFilename())
                .originalFilename(file.getOriginalFilename())
                .fileSize(file.getFileSize())
                .fileType(file.getFileType())
                .mimeType(file.getMimeType())
                .parentFolderId(file.getParentFolder() != null ? file.getParentFolder().getId() : null)
                .parentFolderName(file.getParentFolder() != null ? file.getParentFolder().getFolderName() : null)
                .isFavorite(file.isFavorite())
                .isShared(file.isShared())
                .createdAt(file.getCreatedAt())
                .updatedAt(file.getUpdatedAt())
                .lastAccessed(file.getLastAccessed())
                .downloadUrl("/api/v1/files/download/" + file.getId())
                .thumbnailUrl("/api/v1/files/thumbnail/" + file.getId())
                .owner(file.getUser().getUsername())
                .ownerId(file.getUser().getId())
                .build();
    }

    private FolderResponse mapFolderToFolderResponse(Folder folder) {
        return FolderResponse.builder()
                .id(folder.getId())
                .folderName(folder.getFolderName())
                .parentFolderId(folder.getParentFolder() != null ? folder.getParentFolder().getId() : null)
                .parentFolderName(folder.getParentFolder() != null ? folder.getParentFolder().getFolderName() : null)
                .createdAt(folder.getCreatedAt())
                .updatedAt(folder.getUpdatedAt())
                .lastAccessed(folder.getLastAccessed())
                .isFavorite(folder.isFavorite())
                .hasSharedItems(folder.isShared())
                .owner(folder.getUser().getUsername())
                .ownerId(folder.getUser().getId())
                .build();
    }
}