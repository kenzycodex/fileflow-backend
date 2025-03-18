package com.fileflow.dto.response.file;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileUploadResponse {
    private Long fileId;
    private String filename;
    private String originalFilename;
    private Long fileSize;
    private String fileType;
    private String mimeType;
    private Long parentFolderId;
    private String downloadUrl;
    private String thumbnailUrl;
}