package com.fileflow.dto.request.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for uploading a file chunk
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkUploadRequest {
    @NotNull(message = "Chunk file is required")
    private MultipartFile chunk;

    @NotNull(message = "Chunk number is required")
    @Min(value = 0, message = "Chunk number must be at least 0")
    private Integer chunkNumber;

    @NotNull(message = "Total chunks is required")
    @Min(value = 1, message = "Total chunks must be at least 1")
    private Integer totalChunks;

    @NotNull(message = "Total size is required")
    @Min(value = 1, message = "Total size must be at least 1")
    private Long totalSize;

    private String uploadId;

    private String originalFilename;

    private Long folderId;
}