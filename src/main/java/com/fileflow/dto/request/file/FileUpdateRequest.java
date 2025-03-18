package com.fileflow.dto.request.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Size;

/**
 * DTO for updating file metadata
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUpdateRequest {
    @Size(max = 255, message = "Filename cannot exceed 255 characters")
    private String filename;

    private Long parentFolderId;

    private Boolean isFavorite;
}
