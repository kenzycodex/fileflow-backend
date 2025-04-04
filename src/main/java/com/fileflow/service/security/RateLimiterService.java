package com.fileflow.service.security;

import com.fileflow.config.AppConfig;
import com.fileflow.exception.TooManyRequestsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service for rate limiting API requests
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimiterService {

    private final RedisTemplate<String, String> redisTemplate;
    private final AppConfig appConfig;

    // Rate limit key prefixes
    private static final String RATE_LIMIT_PREFIX = "rate:limit:";
    private static final String LOGIN_LIMIT_PREFIX = "rate:login:";
    private static final String SIGNUP_LIMIT_PREFIX = "rate:signup:";
    private static final String PASSWORD_RESET_LIMIT_PREFIX = "rate:password:reset:";
    private static final String IP_LIMIT_PREFIX = "rate:ip:";

    /**
     * Check if a general API request exceeds rate limits
     *
     * @param key Unique identifier (user ID, IP address, etc.)
     * @throws TooManyRequestsException if rate limit exceeded
     */
    public void checkRateLimit(String key) {
        if (!appConfig.getRateLimiting().isEnabled()) {
            return;
        }

        String limitKey = RATE_LIMIT_PREFIX + key;
        checkLimit(limitKey, appConfig.getRateLimiting().getMaxRequestsPerMinute(), 60);
    }

    /**
     * Check if login attempts exceed rate limits
     *
     * @param key Unique identifier (username, email, or IP)
     * @throws TooManyRequestsException if rate limit exceeded
     */
    public void checkLoginRateLimit(String key) {
        if (!appConfig.getRateLimiting().isEnabled()) {
            return;
        }

        String limitKey = LOGIN_LIMIT_PREFIX + key;
        checkLimit(limitKey, appConfig.getRateLimiting().getMaxLoginAttemptsPerMinute(), 60);
    }

    /**
     * Check if signup attempts exceed rate limits
     *
     * @param key Unique identifier (IP address)
     * @throws TooManyRequestsException if rate limit exceeded
     */
    public void checkSignupRateLimit(String key) {
        if (!appConfig.getRateLimiting().isEnabled()) {
            return;
        }

        String limitKey = SIGNUP_LIMIT_PREFIX + key;
        checkLimit(limitKey, appConfig.getRateLimiting().getMaxSignupAttemptsPerHour(), 3600);
    }

    /**
     * Check if password reset attempts exceed rate limits
     *
     * @param key Unique identifier (email or IP address)
     * @throws TooManyRequestsException if rate limit exceeded
     */
    public void checkPasswordResetRateLimit(String key) {
        if (!appConfig.getRateLimiting().isEnabled()) {
            return;
        }

        String limitKey = PASSWORD_RESET_LIMIT_PREFIX + key;
        checkLimit(limitKey, appConfig.getRateLimiting().getMaxPasswordResetsPerDay(), 86400);
    }

    /**
     * Check if IP address exceeds general rate limits
     *
     * @param ipAddress IP address
     * @throws TooManyRequestsException if rate limit exceeded
     */
    public void checkIpRateLimit(String ipAddress) {
        if (!appConfig.getRateLimiting().isEnabled()) {
            return;
        }

        String limitKey = IP_LIMIT_PREFIX + ipAddress;
        checkLimit(limitKey, appConfig.getRateLimiting().getMaxRequestsPerMinute() * 5, 60);
    }

    /**
     * Generic method to check a rate limit
     *
     * @param key Redis key
     * @param maxRequests Maximum allowed requests in the time period
     * @param timeWindowSeconds Time window in seconds
     * @throws TooManyRequestsException if rate limit exceeded
     */
    private void checkLimit(String key, int maxRequests, int timeWindowSeconds) {
        // Get current count
        Long count = redisTemplate.opsForValue().increment(key);

        // Set expiry if this is the first request
        if (count != null && count == 1) {
            redisTemplate.expire(key, timeWindowSeconds, TimeUnit.SECONDS);
        }

        // Check if rate limit exceeded
        if (count != null && count > maxRequests) {
            // Get remaining time window
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            log.warn("Rate limit exceeded for key: {}, count: {}, max: {}", key, count, maxRequests);

            throw new TooManyRequestsException("Rate limit exceeded. Try again later.", ttl);
        }
    }

    /**
     * Reset a rate limit (e.g., after successful authentication)
     *
     * @param key Unique identifier
     */
    public void resetLimit(String key) {
        redisTemplate.delete(LOGIN_LIMIT_PREFIX + key);
    }
}