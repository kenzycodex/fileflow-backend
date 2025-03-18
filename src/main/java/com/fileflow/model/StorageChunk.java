package com.fileflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Entity representing a chunk of a file during chunked upload
 */
@Data
@Entity
@Table(name = "storage_chunks", indexes = {
        @Index(name = "idx_storage_chunk_upload_id", columnList = "uploadId"),
        @Index(name = "idx_storage_chunk_user_id", columnList = "userId")
})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String uploadId;

    @NotNull
    private Integer chunkNumber;

    @NotNull
    private Integer totalChunks;

    @NotNull
    private Long userId;

    @NotBlank
    private String originalFilename;

    @NotBlank
    private String storagePath;

    @NotNull
    private Long chunkSize;

    @NotNull
    private Long totalSize;

    private String mimeType;

    private Long parentFolderId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;
}