package com.fileflow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Application-wide configuration properties
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {

    // Base URL of the application for email links
    private String baseUrl = "http://localhost:8080";

    // Security settings
    private SecurityConfig security = new SecurityConfig();

    // Email settings
    private EmailConfig email = new EmailConfig();

    // MFA settings
    private MfaConfig mfa = new MfaConfig();

    // Rate limiting settings
    private RateLimitingConfig rateLimiting = new RateLimitingConfig();

    @Data
    public static class SecurityConfig {
        // CORS allowed origins - default to localhost origins instead of wildcard
        private String[] allowedOrigins = {"http://localhost:3000", "http://localhost:4173"};

        // Password strength settings
        private PasswordStrength passwordStrength = new PasswordStrength();

        // Session timeouts
        private long accessTokenExpiration = 3600000; // 1 hour
        private long refreshTokenExpiration = 604800000; // 7 days

        private long rememberMeDurationDays = 14; // 14 days for "Remember Me"
        private long standardDurationHours = 1; // 1 hour for standard session
        private long tokenFamilyMaxAgeDays = 30; // Maximum age for token families
        private int tokenRefreshThresholdSeconds = 300; // Refresh when token has less than 5 minutes left

        // Login attempt limits
        private int maxLoginAttempts = 5;
        private int lockoutDurationMinutes = 15;

        // Enable account lockout
        private boolean accountLockoutEnabled = true;

        // Enable IP tracking
        private boolean trackLoginIp = true;

        // Unusual activity detection
        private boolean detectUnusualActivity = true;
    }

    @Data
    public static class PasswordStrength {
        private int minLength = 8;
        private boolean requireDigits = true;
        private boolean requireLowercase = true;
        private boolean requireUppercase = true;
        private boolean requireSpecial = true;
    }

    @Data
    public static class EmailConfig {
        private boolean enabled = true;
        private String senderName = "FileFlow";
        private long emailVerificationExpiryHours = 24;
        private long passwordResetExpiryHours = 24;
    }

    @Data
    public static class MfaConfig {
        private boolean enabled = false;
        private boolean required = false;
        private int codeLength = 6;
        private int codeExpirySeconds = 30;
        private String issuer = "FileFlow";
        private String qrCodeSize = "200x200";
    }

    @Data
    public static class RateLimitingConfig {
        private boolean enabled = true;
        private int maxRequestsPerMinute = 60;
        private int maxLoginAttemptsPerMinute = 5;
        private int maxSignupAttemptsPerHour = 3;
        private int maxPasswordResetsPerDay = 3;
    }
}