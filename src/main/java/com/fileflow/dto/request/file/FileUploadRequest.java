package com.fileflow.dto.request.file;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO for file uploads
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadRequest {

    @NotNull(message = "File cannot be null")
    private MultipartFile file;

    private Long folderId;

    private Boolean overwrite;
}