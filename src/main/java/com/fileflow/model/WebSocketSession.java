package com.fileflow.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a WebSocket session
 */
@Entity
@Table(name = "websocket_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "session_id", nullable = false, unique = true)
    private String sessionId;

    @Column(name = "connected_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime connectedAt;

    @Column(name = "last_activity", nullable = false)
    private LocalDateTime lastActivity;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "disconnected_at")
    private LocalDateTime disconnectedAt;
}