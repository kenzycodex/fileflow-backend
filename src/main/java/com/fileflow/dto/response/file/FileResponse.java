package com.fileflow.dto.response.file;

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
public class FileResponse {
    private Long id;
    private String filename;
    private String originalFilename;
    private Long fileSize;
    private String fileType;
    private String mimeType;
    private Long parentFolderId;
    private String parentFolderName;
    private boolean isFavorite;
    private boolean isShared;
    private String downloadUrl;
    private String thumbnailUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastAccessed;
    private String owner;
    private Long ownerId;
}