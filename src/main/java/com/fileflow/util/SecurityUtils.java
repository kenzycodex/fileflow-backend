package com.fileflow.util;

import com.fileflow.model.User;
import com.fileflow.security.JwtTokenProvider;
import com.fileflow.service.email.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for security-related functions
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityUtils {

    private final RedisTemplate<String, String> redisTemplate;
    private final EmailService emailService;
    private final JwtTokenProvider tokenProvider;

    private static final String IP_HISTORY_PREFIX = "security:ip:history:";
    private static final String DEVICE_HISTORY_PREFIX = "security:device:history:";
    private static final int MAX_HISTORY_ITEMS = 5;
    private static final int HISTORY_EXPIRY_DAYS = 90;

    /**
     * Get client IP address from request
     */
    public String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            // Get first IP in case of proxy chain
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Get user agent information
     */
    public String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    /**
     * Extract user ID from the JWT token in the request
     *
     * @param request HTTP request containing JWT token
     * @return User ID from token or null if not present/valid
     */
    public Long getUserIdFromRequest(HttpServletRequest request) {
        try {
            String bearerToken = request.getHeader("Authorization");
            if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
                String token = bearerToken.substring(7);
                if (tokenProvider.validateToken(token)) {
                    return tokenProvider.getUserIdFromJWT(token);
                }
            }
            return null;
        } catch (Exception e) {
            log.debug("Could not extract user ID from request: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Track a successful login
     */
    public void trackSuccessfulLogin(User user, String ipAddress, String userAgent) {
        String userId = user.getId().toString();
        String ipKey = IP_HISTORY_PREFIX + userId;
        String deviceKey = DEVICE_HISTORY_PREFIX + userId;

        // Track IP address
        redisTemplate.opsForList().leftPush(ipKey, ipAddress);
        redisTemplate.opsForList().trim(ipKey, 0, MAX_HISTORY_ITEMS - 1);
        redisTemplate.expire(ipKey, HISTORY_EXPIRY_DAYS, TimeUnit.DAYS);

        // Track device info
        if (userAgent != null) {
            redisTemplate.opsForList().leftPush(deviceKey, userAgent);
            redisTemplate.opsForList().trim(deviceKey, 0, MAX_HISTORY_ITEMS - 1);
            redisTemplate.expire(deviceKey, HISTORY_EXPIRY_DAYS, TimeUnit.DAYS);
        }

        // Log the activity
        log.info("Successful login: user={}, ip={}, device={}", userId, ipAddress, userAgent);
    }

    /**
     * Check for suspicious login activity
     *
     * @return true if suspicious activity detected
     */
    public boolean checkSuspiciousActivity(User user, String ipAddress, String userAgent) {
        String userId = user.getId().toString();
        String ipKey = IP_HISTORY_PREFIX + userId;

        // Get IP history
        List<String> ipHistory = redisTemplate.opsForList().range(ipKey, 0, -1);

        // If no history, this might be the first login
        if (ipHistory == null || ipHistory.isEmpty()) {
            return false;
        }

        // Check if this IP is in recent history
        boolean knownIp = ipHistory.contains(ipAddress);

        // If this is a new IP and we detect suspicious login
        if (!knownIp) {
            // Get geolocation info from IP
            String location = getLocationFromIp(ipAddress);

            // Notify user about suspicious login
            emailService.sendUnusualActivityEmail(
                    user,
                    ipAddress,
                    location != null ? location : "Unknown",
                    userAgent != null ? userAgent : "Unknown"
            );

            log.warn("Suspicious login detected: user={}, ip={}, location={}", userId, ipAddress, location);
            return true;
        }

        return false;
    }

    /**
     * Get location information from IP address
     * In a real implementation, this would use a geolocation service
     */
    private String getLocationFromIp(String ipAddress) {
        // This is a placeholder. In a real implementation, you would use a service
        // like MaxMind GeoIP, IPinfo.io, or IP-API to get location data.
        return "Unknown";
    }

    /**
     * Sanitize input to prevent XSS attacks
     */
    public String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }

        // Basic HTML sanitization
        return input.replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("\"", "&quot;")
                .replaceAll("'", "&#x27;")
                .replaceAll("/", "&#x2F;");
    }

    /**
     * Generate a secure random token
     */
    public String generateSecureToken() {
        return java.util.UUID.randomUUID().toString();
    }
}