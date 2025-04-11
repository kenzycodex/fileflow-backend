package com.fileflow.service.security;

import com.fileflow.config.AppConfig;
import com.fileflow.exception.TooManyRequestsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
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
    private final CacheManager cacheManager; // Added for local caching

    // Cache names for local caching
    private static final String RATE_LIMIT_CACHE = "rateLimitCache";
    private static final String RATE_LIMIT_CACHE_EXPIRE = "rateLimitExpireCache";

    // Rate limit key prefixes
    private static final String RATE_LIMIT_PREFIX = "rate:limit:";
    private static final String LOGIN_LIMIT_PREFIX = "rate:login:";
    private static final String SIGNUP_LIMIT_PREFIX = "rate:signup:";
    private static final String PASSWORD_RESET_LIMIT_PREFIX = "rate:password:reset:";
    private static final String IP_LIMIT_PREFIX = "rate:ip:";
    private static final String TOKEN_LIMIT_PREFIX = "rate:token:";

    /**
     * Check if a general API request exceeds rate limits
     *
     * @param key Unique identifier (user ID, IP address, etc.)
     * @return boolean True if under limit, false if rate limited
     */
    public boolean checkRateLimit(String key) {
        if (!appConfig.getRateLimiting().isEnabled()) {
            return true;
        }

        String limitKey = RATE_LIMIT_PREFIX + key;
        try {
            return checkLimitInternal(limitKey, appConfig.getRateLimiting().getMaxRequestsPerMinute(), 60);
        } catch (TooManyRequestsException e) {
            return false;
        }
    }

    /**
     * Check if login attempts exceed rate limits
     *
     * @param key Unique identifier (username, email, or IP)
     * @return boolean True if under limit, false if rate limited
     */
    public boolean checkLoginRateLimit(String key) {
        if (!appConfig.getRateLimiting().isEnabled()) {
            return true;
        }

        String limitKey = LOGIN_LIMIT_PREFIX + key;
        try {
            return checkLimitInternal(limitKey, appConfig.getRateLimiting().getMaxLoginAttemptsPerMinute(), 60);
        } catch (TooManyRequestsException e) {
            return false;
        }
    }

    /**
     * Check if signup attempts exceed rate limits
     *
     * @param key Unique identifier (IP address)
     * @return boolean True if under limit, false if rate limited
     */
    public boolean checkSignupRateLimit(String key) {
        if (!appConfig.getRateLimiting().isEnabled()) {
            return true;
        }

        String limitKey = SIGNUP_LIMIT_PREFIX + key;
        try {
            return checkLimitInternal(limitKey, appConfig.getRateLimiting().getMaxSignupAttemptsPerHour(), 3600);
        } catch (TooManyRequestsException e) {
            return false;
        }
    }

    /**
     * Check if password reset attempts exceed rate limits
     *
     * @param key Unique identifier (email or IP address)
     * @return boolean True if under limit, false if rate limited
     */
    public boolean checkPasswordResetRateLimit(String key) {
        if (!appConfig.getRateLimiting().isEnabled()) {
            return true;
        }

        String limitKey = PASSWORD_RESET_LIMIT_PREFIX + key;
        try {
            return checkLimitInternal(limitKey, appConfig.getRateLimiting().getMaxPasswordResetsPerDay(), 86400);
        } catch (TooManyRequestsException e) {
            return false;
        }
    }

    /**
     * Check if IP address exceeds general rate limits
     *
     * @param ipAddress IP address
     * @return boolean True if under limit, false if rate limited
     */
    public boolean checkIpRateLimit(String ipAddress) {
        if (!appConfig.getRateLimiting().isEnabled()) {
            return true;
        }

        String limitKey = IP_LIMIT_PREFIX + ipAddress;
        try {
            return checkLimitInternal(limitKey, appConfig.getRateLimiting().getMaxRequestsPerMinute() * 5, 60);
        } catch (TooManyRequestsException e) {
            return false;
        }
    }

    /**
     * Generic method to check a rate limit (throws exception if exceeded)
     *
     * @param key Redis key
     * @param maxRequests Maximum allowed requests in the time period
     * @param timeWindowSeconds Time window in seconds
     * @throws TooManyRequestsException if rate limit exceeded
     */
    private boolean checkLimitInternal(String key, int maxRequests, int timeWindowSeconds) {
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

        return true;
    }

    /**
     * Check rate limit for password reset token attempts
     * Prevents brute forcing a specific token
     *
     * @param token Password reset token
     * @return boolean true if under limit, false if rate limited
     */
    public boolean checkResetTokenRateLimit(String token) {
        if (!appConfig.getRateLimiting().isEnabled() || token == null || token.isEmpty()) {
            return true; // Skip rate limiting for disabled or empty tokens
        }

        String key = TOKEN_LIMIT_PREFIX + token;

        // More strict limit: 3 attempts per 15 minutes per token
        // This helps prevent brute forcing a specific token
        int maxAttempts = 3;
        int timeWindowSeconds = 15 * 60; // 15 minutes

        return checkRateLimitInternal(key, maxAttempts, timeWindowSeconds);
    }

    /**
     * Internal helper method to implement rate limiting with consistent logic
     * Using local caching instead of Redis for token-specific limits
     *
     * @param key Cache key to use
     * @param maxAttempts Maximum number of attempts allowed
     * @param timeWindowSeconds Time window in seconds
     * @return boolean true if under limit, false if rate limited
     */
    private boolean checkRateLimitInternal(String key, int maxAttempts, int timeWindowSeconds) {
        try {
            // Get current count and time from local cache
            Integer count = cacheManager.getCache(RATE_LIMIT_CACHE).get(key, Integer.class);
            Long timestamp = cacheManager.getCache(RATE_LIMIT_CACHE_EXPIRE).get(key, Long.class);
            long currentTime = System.currentTimeMillis() / 1000;

            if (count == null || timestamp == null) {
                // First attempt
                cacheManager.getCache(RATE_LIMIT_CACHE).put(key, 1);
                cacheManager.getCache(RATE_LIMIT_CACHE_EXPIRE).put(key, currentTime + timeWindowSeconds);
                return true;
            }

            // Check if time window has expired
            if (currentTime > timestamp) {
                // Reset counter
                cacheManager.getCache(RATE_LIMIT_CACHE).put(key, 1);
                cacheManager.getCache(RATE_LIMIT_CACHE_EXPIRE).put(key, currentTime + timeWindowSeconds);
                return true;
            }

            // Check if under limit
            if (count < maxAttempts) {
                // Increment counter
                cacheManager.getCache(RATE_LIMIT_CACHE).put(key, count + 1);
                return true;
            }

            // Rate limited
            log.warn("Local rate limit exceeded for key: {}, count: {}, max: {}", key, count, maxAttempts);
            return false;
        } catch (Exception e) {
            log.error("Error in rate limiter: {}", e.getMessage());
            // In case of error, allow the request (fail open for usability)
            return true;
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