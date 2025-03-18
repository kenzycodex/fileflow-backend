package com.fileflow.dto.response.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fileflow.dto.response.file.FileResponse;
import com.fileflow.dto.response.folder.FolderResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for search results
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchResponse {
    private List<FileResponse> files;
    private List<FolderResponse> folders;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasMore;
    private String query;
}