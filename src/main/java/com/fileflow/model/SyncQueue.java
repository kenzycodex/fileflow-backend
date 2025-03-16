package com.fileflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sync_queue")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncQueue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long deviceId;

    @Column(nullable = false)
    private String actionType;

    private Long itemId;

    private String itemType;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(nullable = false)
    private LocalDateTime created_at;

    private LocalDateTime processedAt;

    private int retryCount;

    @Column(columnDefinition = "TEXT")
    private String dataPayload;

    public enum Status {
        PENDING,
        COMPLETED,
        FAILED
    }
}
