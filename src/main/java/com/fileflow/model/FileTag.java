package com.fileflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a relationship between a file and a tag
 */
@Data
@Entity
@Table(name = "file_tags",
        uniqueConstraints = @UniqueConstraint(columnNames = {"file_id", "tag_id"}))
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileTag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private File file;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
