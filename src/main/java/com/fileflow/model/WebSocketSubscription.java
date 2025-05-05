package com.fileflow.model;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a WebSocket subscription
 */
@Entity
@Table(name = "websocket_subscriptions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "item_id", "item_type"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "item_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ItemType itemType;

    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    /**
     * Types of items that can be subscribed to
     */
    public enum ItemType {
        FILE,
        FOLDER
    }
}