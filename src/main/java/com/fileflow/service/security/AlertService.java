package com.fileflow.service.security;

import com.fileflow.model.User;
import com.fileflow.repository.UserRepository;
import com.fileflow.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Service to handle security alerts and notifications
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AlertService {

    private final EmailService emailService;
    private final UserRepository userRepository;

    @Value("${app.security.alerts.enabled:true}")
    private boolean alertsEnabled;

    @Value("${app.admin.email:admin@fileflow.com}")
    private String adminEmail;

    /**
     * Send account locked alert
     *
     * @param userId    User ID
     * @param email     User email
     * @param ipAddress IP address that triggered the lock
     * @param reason    Reason for locking
     */
    @Async
    public void sendAccountLockedAlert(Long userId, String email, String ipAddress, String reason) {
        if (!alertsEnabled) {
            log.info("Alerts disabled. Would send account locked alert to: {}", email);
            return;
        }

        try {
            // If we have userId, get the user from repository
            if (userId != null) {
                Optional<User> userOpt = userRepository.findById(userId);

                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    // Send account locked email to user
                    emailService.sendAccountLockedEmail(user, 30); // 30 minutes lockout

                    // Log the alert
                    log.info("Account locked alert sent to user: {}", user.getEmail());
                    return;
                }
            }

            // If we don't have a user object but have an email, create a temporary user
            if (email != null && !email.isEmpty()) {
                User tempUser = new User();
                tempUser.setEmail(email);
                tempUser.setFirstName("User");

                // Send email
                emailService.sendAccountLockedEmail(tempUser, 30);

                // Log the alert
                log.info("Account locked alert sent to email: {}", email);
            }
        } catch (Exception e) {
            log.error("Failed to send account locked alert: {}", e.getMessage(), e);
        }
    }

    /**
     * Send suspicious activity alert
     *
     * @param identifier   Username or email
     * @param ipAddress    IP address
     * @param eventType    Type of event
     * @param attemptCount Number of failed attempts
     */
    @Async
    public void sendSuspiciousActivityAlert(String identifier, String ipAddress, String eventType, int attemptCount) {
        if (!alertsEnabled) {
            log.info("Alerts disabled. Would send suspicious activity alert for: {}", identifier);
            return;
        }

        try {
            // Try to find the user
            Optional<User> userOpt = userRepository.findByEmail(identifier);
            if (userOpt.isEmpty()) {
                userOpt = userRepository.findByUsername(identifier);
            }

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                // Send unusual activity email
                emailService.sendUnusualActivityEmail(
                        user,
                        ipAddress,
                        "Unknown", // Location could be determined using IP geolocation
                        "Unknown device"
                );

                log.info("Suspicious activity alert sent to user: {}", user.getEmail());
            }

            // Also notify admin
            if (adminEmail != null && !adminEmail.isEmpty()) {
                // Create temporary admin user for email
                User adminUser = new User();
                adminUser.setEmail(adminEmail);
                adminUser.setFirstName("Admin");

                // Send email with details
                Map<String, Object> details = new HashMap<>();
                details.put("identifier", identifier);
                details.put("ipAddress", ipAddress);
                details.put("eventType", eventType);
                details.put("attemptCount", attemptCount);

                // Use unusual activity email since it has similar structure
                emailService.sendUnusualActivityEmail(
                        adminUser,
                        ipAddress,
                        "Unknown",
                        "Multiple failed attempts"
                );

                log.info("Suspicious activity alert sent to admin: {}", adminEmail);
            }
        } catch (Exception e) {
            log.error("Failed to send suspicious activity alert: {}", e.getMessage(), e);
        }
    }

    /**
     * Send distributed attack alert
     *
     * @param identifier  Username or email under attack
     * @param ipAddresses List of IP addresses involved
     * @param eventType   Type of event
     */
    @Async
    public void sendDistributedAttackAlert(String identifier, List<String> ipAddresses, String eventType) {
        if (!alertsEnabled) {
            log.info("Alerts disabled. Would send distributed attack alert for: {}", identifier);
            return;
        }

        try {
            // This is higher severity, so mainly alert admins
            if (adminEmail != null && !adminEmail.isEmpty()) {
                // Create temporary admin user for email
                User adminUser = new User();
                adminUser.setEmail(adminEmail);
                adminUser.setFirstName("Admin");

                // Format IP addresses for display
                String ipList = String.join(", ", ipAddresses);

                // Send email with attack details
                emailService.sendUnusualActivityEmail(
                        adminUser,
                        ipList,
                        "Multiple locations",
                        "Possible distributed attack - " + eventType
                );

                log.info("Distributed attack alert sent to admin: {}", adminEmail);
            }

            // Also notify the user if we can identify them
            Optional<User> userOpt = userRepository.findByEmail(identifier);
            if (userOpt.isEmpty()) {
                userOpt = userRepository.findByUsername(identifier);
            }

            if (userOpt.isPresent()) {
                User user = userOpt.get();

                // Send unusual activity email
                emailService.sendUnusualActivityEmail(
                        user,
                        ipAddresses.get(0) + " and others",
                        "Multiple locations",
                        "Suspicious login attempts from multiple locations"
                );

                log.info("Distributed attack alert sent to user: {}", user.getEmail());
            }
        } catch (Exception e) {
            log.error("Failed to send distributed attack alert: {}", e.getMessage(), e);
        }
    }

    /**
     * Send token brute force alert
     *
     * @param tokenType    Type of token
     * @param ipAddress    IP address attempting brute force
     * @param attemptCount Number of attempts
     */
    @Async
    public void sendTokenBruteForceAlert(String tokenType, String ipAddress, int attemptCount) {
        if (!alertsEnabled) {
            log.info("Alerts disabled. Would send token brute force alert for IP: {}", ipAddress);
            return;
        }

        try {
            // This is admin-only alert
            if (adminEmail != null && !adminEmail.isEmpty()) {
                // Create temporary admin user for email
                User adminUser = new User();
                adminUser.setEmail(adminEmail);
                adminUser.setFirstName("Admin");

                // Send email with attack details
                emailService.sendUnusualActivityEmail(
                        adminUser,
                        ipAddress,
                        "Unknown",
                        "Possible token brute force attack - " + tokenType + " (" + attemptCount + " attempts)"
                );

                log.info("Token brute force alert sent to admin: {}", adminEmail);
            }
        } catch (Exception e) {
            log.error("Failed to send token brute force alert: {}", e.getMessage(), e);
        }
    }

    /**
     * Send new location login alert
     *
     * @param userId    User ID
     * @param ipAddress New IP address
     */
    @Async
    public void sendNewLocationLoginAlert(Long userId, String ipAddress) {
        if (!alertsEnabled || userId == null) {
            log.info("Alerts disabled or missing user ID. Would send new location alert for user: {}", userId);
            return;
        }

        try {
            // Find the user
            Optional<User> userOpt = userRepository.findById(userId);

            if (userOpt.isPresent()) {
                User user = userOpt.get();

                // Send unusual activity email - but with lower severity tone
                emailService.sendUnusualActivityEmail(
                        user,
                        ipAddress,
                        "New location",
                        "Login from a new location"
                );

                log.info("New location login alert sent to user: {}", user.getEmail());
            }
        } catch (Exception e) {
            log.error("Failed to send new location login alert: {}", e.getMessage(), e);
        }
    }
}