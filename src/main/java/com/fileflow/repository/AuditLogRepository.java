package com.fileflow.repository;

import com.fileflow.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for AuditLog entity
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Find audit logs by user ID
     *
     * @param userId User ID
     * @return List of audit logs
     */
    List<AuditLog> findByUserIdOrderByEventTimeDesc(Long userId);

    /**
     * Find audit logs by event type
     *
     * @param eventType Event type
     * @return List of audit logs
     */
    List<AuditLog> findByEventTypeOrderByEventTimeDesc(String eventType);

    /**
     * Find audit logs by IP address
     *
     * @param ipAddress IP address
     * @return List of audit logs
     */
    List<AuditLog> findByIpAddressOrderByEventTimeDesc(String ipAddress);

    /**
     * Find recent failed attempts for a specific identifier (email or username)
     *
     * @param eventType Event type
     * @param identifier Email or username
     * @param after Time threshold
     * @return List of failed audit logs
     */
    @Query("SELECT a FROM AuditLog a WHERE a.eventType = :eventType AND (a.email = :identifier OR a.username = :identifier) AND a.eventStatus = 'FAILURE' AND a.eventTime > :after ORDER BY a.eventTime DESC")
    List<AuditLog> findRecentFailedAttempts(
            @Param("eventType") String eventType,
            @Param("identifier") String identifier,
            @Param("after") LocalDateTime after
    );

    /**
     * Find unique IP addresses for failed attempts on a specific identifier
     *
     * @param eventType Event type
     * @param identifier Email or username
     * @param after Time threshold
     * @return List of unique IP addresses
     */
    @Query("SELECT DISTINCT a.ipAddress FROM AuditLog a WHERE a.eventType = :eventType AND (a.email = :identifier OR a.username = :identifier) AND a.eventStatus = 'FAILURE' AND a.eventTime > :after")
    List<String> findUniqueIPsForFailedAttempts(
            @Param("eventType") String eventType,
            @Param("identifier") String identifier,
            @Param("after") LocalDateTime after
    );

    /**
     * Find recent token validation attempts from a specific IP address
     *
     * @param eventType Token validation event type
     * @param ipAddress IP address
     * @param after Time threshold
     * @return List of audit logs
     */
    @Query("SELECT a FROM AuditLog a WHERE a.eventType = :eventType AND a.ipAddress = :ipAddress AND a.eventStatus = 'FAILURE' AND a.eventTime > :after ORDER BY a.eventTime DESC")
    List<AuditLog> findRecentTokenValidationAttempts(
            @Param("eventType") String eventType,
            @Param("ipAddress") String ipAddress,
            @Param("after") LocalDateTime after
    );

    /**
     * Find common IP addresses for a user's successful logins
     *
     * @param userId User ID
     * @param eventType Event type (usually LOGIN)
     * @param after Time threshold
     * @return List of IP addresses
     */
    @Query("SELECT DISTINCT a.ipAddress FROM AuditLog a WHERE a.userId = :userId AND a.eventType = :eventType AND a.eventStatus = 'SUCCESS' AND a.eventTime > :after")
    List<String> findCommonIPsForUser(
            @Param("userId") Long userId,
            @Param("eventType") String eventType,
            @Param("after") LocalDateTime after
    );
}