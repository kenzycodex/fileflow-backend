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
public class FileVersionResponse {
    private Long id;
    private Long fileId;
    private int versionNumber;
    private Long fileSize;
    private LocalDateTime createdAt;
    private String createdBy;
    private Long createdById;
    private String comment;
    private String downloadUrl;
}