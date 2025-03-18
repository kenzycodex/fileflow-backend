package com.fileflow.dto.response.folder;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FolderResponse {
    private Long id;
    private String folderName;
    private Long parentFolderId;
    private String parentFolderName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastAccessed;
    private String owner;
    private Long ownerId;
    private Integer fileCount;
    private Integer subfolderCount;
    private String path;
    private boolean isFavorite;
    private boolean hasSharedItems;
}