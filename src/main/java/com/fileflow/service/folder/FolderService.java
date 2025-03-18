package com.fileflow.service.folder;

import com.fileflow.dto.request.folder.FolderCreateRequest;
import com.fileflow.dto.request.folder.FolderUpdateRequest;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.folder.FolderContentsResponse;
import com.fileflow.dto.response.folder.FolderResponse;

import java.util.List;

public interface FolderService {
    /**
     * Create a new folder
     */
    FolderResponse createFolder(FolderCreateRequest folderCreateRequest);

    /**
     * Get folder by ID
     */
    FolderResponse getFolder(Long folderId);

    /**
     * Get folder contents (files and subfolders)
     */
    FolderContentsResponse getFolderContents(Long folderId);

    /**
     * Update folder metadata
     */
    FolderResponse updateFolder(Long folderId, FolderUpdateRequest folderUpdateRequest);

    /**
     * Move folder to trash
     */
    ApiResponse deleteFolder(Long folderId);

    /**
     * Restore folder from trash
     */
    ApiResponse restoreFolder(Long folderId);

    /**
     * Permanently delete folder and all its contents
     */
    ApiResponse permanentDeleteFolder(Long folderId);

    /**
     * Move folder to another parent folder
     */
    FolderResponse moveFolder(Long folderId, Long destinationFolderId);

    /**
     * Copy folder to another parent folder
     */
    FolderResponse copyFolder(Long folderId, Long destinationFolderId);

    /**
     * Get user's root folders
     */
    List<FolderResponse> getRootFolders();

    /**
     * Get recent folders
     */
    List<FolderResponse> getRecentFolders(int limit);

    /**
     * Create root folder for a new user
     */
    void createUserRootFolder(Long userId);

    /**
     * Get folder path (breadcrumbs)
     */
    List<FolderResponse> getFolderPath(Long folderId);

    /**
     * Get favorite folders
     */
    List<FolderResponse> getFavoriteFolders();
}