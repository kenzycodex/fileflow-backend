package com.fileflow.repository;

import com.fileflow.model.User;
import com.fileflow.model.WebSocketSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for WebSocket sessions
 */
@Repository
public interface WebSocketSessionRepository extends JpaRepository<WebSocketSession, Long> {

    /**
     * Find a session by its ID
     */
    Optional<WebSocketSession> findBySessionId(String sessionId);

    /**
     * Find all active sessions for a user
     */
    List<WebSocketSession> findByUserAndIsActiveTrue(User user);

    /**
     * Find all active sessions by user ID
     */
    List<WebSocketSession> findByUser_IdAndIsActiveTrue(Long userId);

    /**
     * Count active sessions for a user
     */
    long countByUserAndIsActiveTrue(User user);

    /**
     * Update session activity time
     */
    @Modifying
    @Query("UPDATE WebSocketSession w SET w.lastActivity = :timestamp WHERE w.sessionId = :sessionId")
    void updateLastActivity(@Param("sessionId") String sessionId, @Param("timestamp") LocalDateTime timestamp);

    /**
     * Mark a session as inactive with disconnection time
     */
    @Modifying
    @Query("UPDATE WebSocketSession w SET w.isActive = false, w.disconnectedAt = :timestamp WHERE w.sessionId = :sessionId")
    void markAsDisconnected(@Param("sessionId") String sessionId, @Param("timestamp") LocalDateTime timestamp);

    /**
     * Find inactive sessions older than a given time
     */
    List<WebSocketSession> findByIsActiveFalseAndDisconnectedAtBefore(LocalDateTime timestamp);

    /**
     * Find active sessions with no activity after a given time
     */
    List<WebSocketSession> findByIsActiveTrueAndLastActivityBefore(LocalDateTime timestamp);
}