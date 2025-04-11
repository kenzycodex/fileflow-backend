package com.fileflow.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity to store security and activity audit logs
 */
@Entity
@Table(name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_event_time", columnList = "event_time"),
                @Index(name = "idx_audit_user_id", columnList = "user_id"),
                @Index(name = "idx_audit_event_type", columnList = "event_type"),
                @Index(name = "idx_audit_ip_address", columnList = "ip_address"),
                @Index(name = "idx_audit_email", columnList = "email")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Type of event (LOGIN, LOGOUT, PASSWORD_RESET, etc.)
     */
    @Column(name = "event_type", nullable = false)
    private String eventType;

    /**
     * Status of the event (SUCCESS, FAILURE, WARNING)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_status", nullable = false)
    private EventStatus eventStatus;

    /**
     * Time when the event occurred
     */
    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;

    /**
     * IP address from which the event originated
     */
    @Column(name = "ip_address")
    private String ipAddress;

    /**
     * User agent used for the event
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * User ID associated with the event (can be null for anonymous events)
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * Username associated with the event (can be null)
     */
    @Column(name = "username")
    private String username;

    /**
     * Email associated with the event (can be null)
     */
    @Column(name = "email")
    private String email;

    /**
     * Additional details about the event
     */
    @Column(name = "details", length = 1000)
    private String details;

    /**
     * Session ID for tracking events within a session
     */
    @Column(name = "session_id")
    private String sessionId;

    /**
     * Enumeration for event status
     */
    public enum EventStatus {
        SUCCESS,
        FAILURE,
        WARNING,
        INFO
    }
}