package com.fileflow.service.security;

import com.fileflow.model.AuditLog;
import com.fileflow.model.User;
import com.fileflow.repository.AuditLogRepository;
import com.fileflow.repository.UserRepository;
import com.fileflow.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for logging security-related audit events
 * Provides comprehensive audit logging capabilities for security events
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final Optional<AlertService> alertService; // Optional: may not exist in all environments

    @Value("${app.security.audit.enabled:true}")
    private boolean auditEnabled;

    @Value("${app.security.audit.suspicious-threshold:3}")
    private int suspiciousThreshold;

    /**
     * Log password reset attempt
     *
     * @param success Whether the reset was successful
     * @param email Email address associated with the reset
     * @param ipAddress IP address of the client
     * @param userAgent User agent of the client
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logPasswordReset(boolean success, String email, String ipAddress, String userAgent) {
        if (!auditEnabled) {
            // Just log to file if audit DB is disabled
            String status = success ? "SUCCESS" : "FAILURE";
            String sanitizedEmail = email != null ? email : "unknown";
            String logMessage = String.format(
                    "SECURITY_AUDIT [%s] Password reset %s from IP: %s, Email: %s, UserAgent: %s",
                    LocalDateTime.now(),
                    status,
                    ipAddress,
                    sanitizedEmail,
                    userAgent
            );

            log.info(logMessage);
            return;
        }

        try {
            // Find user by email if possible
            Optional<User> userOpt = userRepository.findByEmail(email);

            AuditLog auditLog = new AuditLog();
            auditLog.setEventType(Constants.ACTIVITY_PASSWORD_RESET);
            auditLog.setEventStatus(success ? AuditLog.EventStatus.SUCCESS : AuditLog.EventStatus.FAILURE);
            auditLog.setEventTime(LocalDateTime.now());
            auditLog.setIpAddress(ipAddress);
            auditLog.setUserAgent(userAgent);
            auditLog.setDetails("Password reset attempt for email: " + email);

            // Set user ID if available
            userOpt.ifPresent(user -> {
                auditLog.setUserId(user.getId());
                auditLog.setUsername(user.getUsername());
                auditLog.setEmail(email);
            });

            // If user not found but email provided
            if (userOpt.isEmpty() && email != null && !email.isBlank()) {
                auditLog.setEmail(email);
            }

            auditLog.setSessionId(UUID.randomUUID().toString()); // Random session ID for tracking

            // Save to database
            auditLogRepository.save(auditLog);

            // Check for suspicious activity patterns
            if (!success) {
                checkForSuspiciousActivity(email, ipAddress, Constants.ACTIVITY_PASSWORD_RESET);
            }
        } catch (Exception e) {
            // Don't let audit logging failures disrupt the application
            log.error("Failed to log password reset audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * Log successful sign-in
     *
     * @param userId User ID
     * @param email Email address
     * @param ipAddress IP address of the client
     * @param userAgent User agent of the client
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSuccessfulSignIn(Long userId, String email, String ipAddress, String userAgent) {
        if (!auditEnabled) {
            String logMessage = String.format(
                    "SECURITY_AUDIT [%s] Successful login for user ID: %d, Email: %s, IP: %s, UserAgent: %s",
                    LocalDateTime.now(),
                    userId,
                    email,
                    ipAddress,
                    userAgent
            );

            log.info(logMessage);
            return;
        }

        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setEventType(Constants.ACTIVITY_LOGIN);
            auditLog.setEventStatus(AuditLog.EventStatus.SUCCESS);
            auditLog.setEventTime(LocalDateTime.now());
            auditLog.setIpAddress(ipAddress);
            auditLog.setUserAgent(userAgent);
            auditLog.setUserId(userId);
            auditLog.setEmail(email);

            // Get user details if available
            userRepository.findById(userId).ifPresent(user -> {
                auditLog.setUsername(user.getUsername());
            });

            auditLog.setSessionId(UUID.randomUUID().toString()); // Generate session ID

            // Save to database
            auditLogRepository.save(auditLog);

            // Check for location-based anomalies
            checkLocationBasedAnomalies(userId, ipAddress);
        } catch (Exception e) {
            log.error("Failed to log successful sign-in audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * Log failed sign-in
     *
     * @param email Email address or username that was attempted
     * @param ipAddress IP address of the client
     * @param userAgent User agent of the client
     * @param reason Reason for the failure
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailedSignIn(String email, String ipAddress, String userAgent, String reason) {
        if (!auditEnabled) {
            String sanitizedEmail = email != null ? email : "unknown";
            String sanitizedReason = reason != null ? reason : "Unknown reason";

            String logMessage = String.format(
                    "SECURITY_AUDIT [%s] Failed login attempt for Email: %s, IP: %s, Reason: %s, UserAgent: %s",
                    LocalDateTime.now(),
                    sanitizedEmail,
                    ipAddress,
                    sanitizedReason,
                    userAgent
            );

            log.warn(logMessage);
            return;
        }

        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setEventType(Constants.ACTIVITY_LOGIN);
            auditLog.setEventStatus(AuditLog.EventStatus.FAILURE);
            auditLog.setEventTime(LocalDateTime.now());
            auditLog.setIpAddress(ipAddress);
            auditLog.setUserAgent(userAgent);
            auditLog.setDetails("Reason: " + (reason != null ? reason : "Unknown"));

            // Try to find user by email
            Optional<User> userOpt = userRepository.findByEmail(email);
            userOpt.ifPresent(user -> {
                auditLog.setUserId(user.getId());
                auditLog.setUsername(user.getUsername());
                auditLog.setEmail(user.getEmail());
            });

            // If not found but email provided
            if (userOpt.isEmpty() && email != null && !email.isBlank()) {
                auditLog.setEmail(email);

                // Also check if it's a username instead
                userRepository.findByUsername(email).ifPresent(user -> {
                    auditLog.setUserId(user.getId());
                    auditLog.setUsername(user.getUsername());
                    auditLog.setEmail(user.getEmail());
                });
            }

            // Save to database
            auditLogRepository.save(auditLog);

            // Check for suspicious activity patterns
            checkForSuspiciousActivity(email, ipAddress, Constants.ACTIVITY_LOGIN);
        } catch (Exception e) {
            log.error("Failed to log failed sign-in audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * Log token validation attempt
     *
     * @param tokenType Type of token (e.g., "RESET", "VERIFICATION", "JWT")
     * @param success Whether validation was successful
     * @param ipAddress IP address of the client
     * @param userAgent User agent of the client
     * @param userId User ID if known (can be null)
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logTokenValidation(String tokenType, boolean success, String ipAddress, String userAgent, Long userId) {
        if (!auditEnabled) {
            String status = success ? "SUCCESS" : "FAILURE";
            String logMessage = String.format(
                    "SECURITY_AUDIT [%s] Token validation (%s) %s from IP: %s, User ID: %s",
                    LocalDateTime.now(),
                    tokenType,
                    status,
                    ipAddress,
                    userId != null ? userId.toString() : "unknown"
            );

            log.info(logMessage);
            return;
        }

        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setEventType("TOKEN_VALIDATION_" + tokenType);
            auditLog.setEventStatus(success ? AuditLog.EventStatus.SUCCESS : AuditLog.EventStatus.FAILURE);
            auditLog.setEventTime(LocalDateTime.now());
            auditLog.setIpAddress(ipAddress);
            auditLog.setUserAgent(userAgent);

            // Set user ID if available
            if (userId != null) {
                auditLog.setUserId(userId);

                // Try to get user details
                userRepository.findById(userId).ifPresent(user -> {
                    auditLog.setUsername(user.getUsername());
                    auditLog.setEmail(user.getEmail());
                });
            }

            // Save to database
            auditLogRepository.save(auditLog);

            // Check for token brute force attempts if validation failed
            if (!success && ipAddress != null) {
                checkForTokenBruteForce(tokenType, ipAddress);
            }
        } catch (Exception e) {
            log.error("Failed to log token validation audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * Log user logout event
     *
     * @param userId User ID
     * @param ipAddress IP address
     * @param userAgent User agent
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLogout(Long userId, String ipAddress, String userAgent) {
        if (!auditEnabled) {
            String logMessage = String.format(
                    "SECURITY_AUDIT [%s] Logout for user ID: %d, IP: %s",
                    LocalDateTime.now(),
                    userId,
                    ipAddress
            );

            log.info(logMessage);
            return;
        }

        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setEventType(Constants.ACTIVITY_LOGOUT);
            auditLog.setEventStatus(AuditLog.EventStatus.SUCCESS);
            auditLog.setEventTime(LocalDateTime.now());
            auditLog.setIpAddress(ipAddress);
            auditLog.setUserAgent(userAgent);
            auditLog.setUserId(userId);

            // Get user details if available
            userRepository.findById(userId).ifPresent(user -> {
                auditLog.setUsername(user.getUsername());
                auditLog.setEmail(user.getEmail());
            });

            // Save to database
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to log logout audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * Log account locked event
     *
     * @param userId User ID
     * @param email User email
     * @param ipAddress IP address that triggered the lock
     * @param reason Reason for locking the account
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAccountLocked(Long userId, String email, String ipAddress, String reason) {
        if (!auditEnabled) {
            String logMessage = String.format(
                    "SECURITY_AUDIT [%s] Account locked for user ID: %d, Email: %s, IP: %s, Reason: %s",
                    LocalDateTime.now(),
                    userId,
                    email,
                    ipAddress,
                    reason
            );

            log.warn(logMessage);
            return;
        }

        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setEventType("ACCOUNT_LOCKED");
            auditLog.setEventStatus(AuditLog.EventStatus.WARNING);
            auditLog.setEventTime(LocalDateTime.now());
            auditLog.setIpAddress(ipAddress);
            auditLog.setUserId(userId);
            auditLog.setEmail(email);
            auditLog.setDetails("Reason: " + reason);

            // Get user details if available
            if (userId != null) {
                userRepository.findById(userId).ifPresent(user -> {
                    auditLog.setUsername(user.getUsername());
                    if (email == null) {
                        auditLog.setEmail(user.getEmail());
                    }
                });
            }

            // Save to database
            auditLogRepository.save(auditLog);

            // Alert about account lockout
            alertService.ifPresent(service -> {
                service.sendAccountLockedAlert(userId, email, ipAddress, reason);
            });
        } catch (Exception e) {
            log.error("Failed to log account locked audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * Check for suspicious activity patterns
     *
     * @param identifier Email or username
     * @param ipAddress IP address
     * @param eventType Type of event to check
     */
    private void checkForSuspiciousActivity(String identifier, String ipAddress, String eventType) {
        if (identifier == null || ipAddress == null) {
            return;
        }

        try {
            // Get recent failed attempts for this identifier
            LocalDateTime threshold = LocalDateTime.now().minusHours(1);
            List<AuditLog> recentAttempts = auditLogRepository.findRecentFailedAttempts(
                    eventType,
                    identifier,
                    threshold
            );

            // Check if threshold exceeded
            if (recentAttempts.size() >= suspiciousThreshold) {
                log.warn("Suspicious activity detected: {} failed {} attempts for {} from IP: {}",
                        recentAttempts.size(), eventType, identifier, ipAddress);

                // Send alert if AlertService exists
                alertService.ifPresent(service -> {
                    service.sendSuspiciousActivityAlert(identifier, ipAddress, eventType, recentAttempts.size());
                });
            }

            // Check for distributed attacks (same identifier, different IPs)
            List<String> uniqueIPs = auditLogRepository.findUniqueIPsForFailedAttempts(
                    eventType,
                    identifier,
                    threshold
            );

            if (uniqueIPs.size() >= 3) { // Configurable threshold for distributed attacks
                log.warn("Possible distributed attack detected: {} different IPs attempting {} for {}",
                        uniqueIPs.size(), eventType, identifier);

                // Send distributed attack alert
                alertService.ifPresent(service -> {
                    service.sendDistributedAttackAlert(identifier, uniqueIPs, eventType);
                });
            }
        } catch (Exception e) {
            log.error("Error checking for suspicious activity: {}", e.getMessage(), e);
        }
    }

    /**
     * Check for token brute force attempts
     *
     * @param tokenType Type of token
     * @param ipAddress IP address
     */
    private void checkForTokenBruteForce(String tokenType, String ipAddress) {
        try {
            // Get recent failed token validations from this IP
            LocalDateTime threshold = LocalDateTime.now().minusMinutes(15);
            List<AuditLog> recentAttempts = auditLogRepository.findRecentTokenValidationAttempts(
                    "TOKEN_VALIDATION_" + tokenType,
                    ipAddress,
                    threshold
            );

            // Check if threshold exceeded
            if (recentAttempts.size() >= 5) { // Configurable threshold
                log.warn("Possible token brute force detected: {} failed {} token validations from IP: {}",
                        recentAttempts.size(), tokenType, ipAddress);

                // Send alert if AlertService exists
                alertService.ifPresent(service -> {
                    service.sendTokenBruteForceAlert(tokenType, ipAddress, recentAttempts.size());
                });
            }
        } catch (Exception e) {
            log.error("Error checking for token brute force: {}", e.getMessage(), e);
        }
    }

    /**
     * Check for location-based anomalies (login from unusual location)
     *
     * @param userId User ID
     * @param ipAddress Current IP address
     */
    private void checkLocationBasedAnomalies(Long userId, String ipAddress) {
        try {
            // Get user's common IP addresses
            List<String> commonIPs = auditLogRepository.findCommonIPsForUser(
                    userId,
                    Constants.ACTIVITY_LOGIN,
                    LocalDateTime.now().minusDays(30)
            );

            // If this is a new IP address and user has logged in before
            if (!commonIPs.contains(ipAddress) && !commonIPs.isEmpty()) {
                log.info("Login from new IP address for user {}: {}", userId, ipAddress);

                // Optional: could implement IP geolocation here to compare locations

                // Send alert for login from new location
                alertService.ifPresent(service -> {
                    service.sendNewLocationLoginAlert(userId, ipAddress);
                });
            }
        } catch (Exception e) {
            log.error("Error checking for location anomalies: {}", e.getMessage(), e);
        }
    }
}