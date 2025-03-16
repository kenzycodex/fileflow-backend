package com.fileflow.service.search;

import com.fileflow.dto.response.common.SearchResponse;
import com.fileflow.dto.response.file.FileResponse;
import com.fileflow.dto.response.folder.FolderResponse;
import com.fileflow.exception.BadRequestException;
import com.fileflow.model.File;
import com.fileflow.model.Folder;
import com.fileflow.model.User;
import com.fileflow.repository.FileRepository;
import com.fileflow.repository.FolderRepository;
import com.fileflow.repository.UserRepository;
import com.fileflow.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService {

    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;

    @Override
    public SearchResponse search(String query, int page, int size) {
        if (!StringUtils.hasText(query)) {
            throw new BadRequestException("Search query cannot be empty");
        }

        User currentUser = getCurrentUser();
        validatePageNumberAndSize(page, size);

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

        // For now, only files can be favorites
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "lastAccessed");
        Page<File> favoriteFiles = fileRepository.findFavorites(currentUser, pageable);

        // Map results
        List<FileResponse> fileResponses = favoriteFiles.getContent().stream()
                .map(this::mapFileToFileResponse)
                .collect(Collectors.toList());

        return SearchResponse.builder()
                .files(fileResponses)
                .folders(Collections.emptyList())
                .page(page)
                .size(size)
                .totalElements(favoriteFiles.getTotalElements())
                .totalPages(favoriteFiles.getTotalPages())
                .hasMore(page < favoriteFiles.getTotalPages() - 1)
                .build();
    }

    @Override
    public SearchResponse searchFiles(String query, int page, int size) {
        if (!StringUtils.hasText(query)) {
            throw new BadRequestException("Search query cannot be empty");
        }

        User currentUser = getCurrentUser();
        validatePageNumberAndSize(page, size);

        // Search files
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

        // Search files by type
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
                .createdAt(file.getCreatedAt())
                .updatedAt(file.getUpdatedAt())
                .lastAccessed(file.getLastAccessed())
                .downloadUrl("/api/v1/files/download/" + file.getId())
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
                .owner(folder.getUser().getUsername())
                .ownerId(folder.getUser().getId())
                .build();
    }
}