package com.fileflow.dto.request.file;

import com.fileflow.validation.annotation.ValidFilename;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for file updates
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUpdateRequest {

    @ValidFilename
    @Size(max = 255, message = "Filename cannot exceed 255 characters")
    private String filename;

    private Long parentFolderId;

    private Boolean isFavorite;
}