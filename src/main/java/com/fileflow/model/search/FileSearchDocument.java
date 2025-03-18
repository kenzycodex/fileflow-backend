package com.fileflow.model.search;

import com.fileflow.model.File;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Document for Elasticsearch indexing of files
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "files")
public class FileSearchDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String filename;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String originalFilename;

    @Field(type = FieldType.Text)
    private String fileType;

    @Field(type = FieldType.Text)
    private String mimeType;

    @Field(type = FieldType.Long)
    private Long fileSize;

    @Field(type = FieldType.Long)
    private Long userId;

    @Field(type = FieldType.Text)
    private String username;

    @Field(type = FieldType.Long)
    private Long parentFolderId;

    @Field(type = FieldType.Text)
    private String parentFolderName;

    @Field(type = FieldType.Boolean)
    private boolean isFavorite;

    @Field(type = FieldType.Boolean)
    private boolean isDeleted;

    @Field(type = FieldType.Date)
    private LocalDateTime createdAt;

    @Field(type = FieldType.Date)
    private LocalDateTime updatedAt;

    @Field(type = FieldType.Date)
    private LocalDateTime lastAccessed;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String content;

    @Field(type = FieldType.Keyword)
    private Set<String> tags = new HashSet<>();

    /**
     * Convert File entity to FileSearchDocument
     */
    public static FileSearchDocument fromEntity(File file, String extractedContent) {
        FileSearchDocument document = FileSearchDocument.builder()
                .id(file.getId())
                .filename(file.getFilename())
                .originalFilename(file.getOriginalFilename())
                .fileType(file.getFileType())
                .mimeType(file.getMimeType())
                .fileSize(file.getFileSize())
                .userId(file.getUser().getId())
                .username(file.getUser().getUsername())
                .isFavorite(file.isFavorite())
                .isDeleted(file.isDeleted())
                .createdAt(file.getCreatedAt())
                .updatedAt(file.getUpdatedAt())
                .lastAccessed(file.getLastAccessed())
                .content(extractedContent)
                .build();

        // Set parent folder information if available
        if (file.getParentFolder() != null) {
            document.setParentFolderId(file.getParentFolder().getId());
            document.setParentFolderName(file.getParentFolder().getFolderName());
        }

        return document;
    }
}