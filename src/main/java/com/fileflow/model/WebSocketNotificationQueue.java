package com.fileflow.model;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a queued WebSocket notification
 */
@Entity
@Table(name = "websocket_notifications_queue")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketNotificationQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "notification_type", nullable = false)
    private String notificationType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "is_sent", nullable = false)
    private boolean isSent = false;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "last_retry")
    private LocalDateTime lastRetry;
}