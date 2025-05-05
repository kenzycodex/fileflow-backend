package com.fileflow.dto.request.file;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO for chunked uploads
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkUploadRequest {

    @NotNull(message = "Chunk cannot be null")
    private MultipartFile chunk;

    private String uploadId;

    @NotNull(message = "Chunk number is required")
    @Min(value = 0, message = "Chunk number must be at least 0")
    private Integer chunkNumber;

    @NotNull(message = "Total chunks is required")
    @Min(value = 1, message = "Total chunks must be at least 1")
    private Integer totalChunks;

    @NotNull(message = "Total size is required")
    @Min(value = 1, message = "Total size must be greater than 0")
    private Long totalSize;

    @NotNull(message = "Original filename is required")
    @Size(min = 1, max = 255, message = "Filename must be between 1 and 255 characters")
    private String originalFilename;

    private Long folderId;
}