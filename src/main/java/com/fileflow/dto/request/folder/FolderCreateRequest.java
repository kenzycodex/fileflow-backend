package com.fileflow.dto.request.folder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolderCreateRequest {

    @NotBlank(message = "Folder name is required")
    @Size(max = 255, message = "Folder name must be less than 255 characters")
    private String folderName;

    private Long parentFolderId;
}