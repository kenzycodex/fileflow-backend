package com.fileflow.dto.response.folder;

import com.fileflow.dto.response.file.FileResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FolderContentsResponse {
    private Long folderId;
    private String folderName;
    private Long parentFolderId;
    private String parentFolderName;
    private List<FileResponse> files;
    private List<FolderResponse> folders;
    private Integer totalItems;
    private Integer fileCount;
    private Integer folderCount;
}