package com.fileflow.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service to manage JWT tokens using Redis for persistence.
 * Maintains token state across server restarts and supports distributed deployments.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class JwtService {
    private final RedisTemplate<String, String> redisTemplate;

    // Redis key prefixes
    private static final String ACCESS_TOKEN_PREFIX = "token:access:";
    private static final String REFRESH_TOKEN_PREFIX = "token:refresh:";
    private static final String USER_TOKENS_PREFIX = "user:tokens:";
    private static final String BLACKLISTED_TOKEN_PREFIX = "token:blacklisted:";
    private static final String FAILED_LOGIN_PREFIX = "login:failed:";
    private static final String USER_LOCKOUT_PREFIX = "user:lockout:";
    private static final String TOKEN_FAMILY_PREFIX = "token:family:";
    private static final String REMEMBER_ME_PREFIX = "token:remember:";

    // Max login attempts before lockout
    private static final int MAX_FAILED_ATTEMPTS = 5;
    // Lockout duration in minutes
    private static final int LOCKOUT_DURATION_MINUTES = 15;
    // Max absolute token lifetime (30 days)
    private static final long MAX_TOKEN_LIFETIME = 30 * 24 * 60 * 60;

    /**
     * Save access token for a user
     */
    public void saveAccessToken(Long userId, String accessToken) {
        saveAccessToken(userId, accessToken, MAX_TOKEN_LIFETIME * 1000);
    }

    /**
     * Save access token for a user with custom expiration
     */
    public void saveAccessToken(Long userId, String accessToken, long expirationMs) {
        String userKey = USER_TOKENS_PREFIX + userId;
        String tokenKey = ACCESS_TOKEN_PREFIX + userId;

        // Store token with expiration
        redisTemplate.opsForValue().set(
                tokenKey,
                accessToken,
                expirationMs,
                TimeUnit.MILLISECONDS
        );

        // Add to user's token collection
        redisTemplate.opsForSet().add(userKey, tokenKey);

        log.debug("Saved access token for user: {} with expiration: {} ms", userId, expirationMs);
    }

    /**
     * Save refresh token for a user
     */
    public void saveRefreshToken(Long userId, String refreshToken) {
        saveRefreshToken(userId, refreshToken, MAX_TOKEN_LIFETIME * 1000);
    }

    /**
     * Save refresh token for a user with custom expiration
     */
    public void saveRefreshToken(Long userId, String refreshToken, long expirationMs) {
        String userKey = USER_TOKENS_PREFIX + userId;
        String tokenKey = REFRESH_TOKEN_PREFIX + userId;

        // Store token with custom expiration
        redisTemplate.opsForValue().set(
                tokenKey,
                refreshToken,
                expirationMs,
                TimeUnit.MILLISECONDS
        );

        // Add to user's token collection
        redisTemplate.opsForSet().add(userKey, tokenKey);

        // Add a marker if this is a long-lived "remember me" token
        if (expirationMs > 24 * 60 * 60 * 1000) { // More than 1 day
            redisTemplate.opsForValue().set(
                    REMEMBER_ME_PREFIX + refreshToken,
                    "true",
                    expirationMs,
                    TimeUnit.MILLISECONDS
            );
        }

        log.debug("Saved refresh token for user: {} with expiration: {} ms", userId, expirationMs);
    }

    /**
     * Get the latest access token for a user
     */
    public String getLatestAccessToken(Long userId) {
        String tokenKey = ACCESS_TOKEN_PREFIX + userId;
        return redisTemplate.opsForValue().get(tokenKey);
    }

    /**
     * Validate if the refresh token exists and matches for the user
     */
    public boolean validateRefreshToken(Long userId, String refreshToken) {
        String tokenKey = REFRESH_TOKEN_PREFIX + userId;
        String storedToken = redisTemplate.opsForValue().get(tokenKey);
        return storedToken != null && storedToken.equals(refreshToken);
    }

    /**
     * Check if a token is blacklisted
     */
    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLISTED_TOKEN_PREFIX + token));
    }

    /**
     * Check if a token is a "remember me" token
     */
    public boolean isRememberMeToken(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(REMEMBER_ME_PREFIX + token));
    }

    /**
     * Blacklist a token
     * @param token The token to blacklist
     * @param expirationMs Expiration time in milliseconds
     */
    public void blacklistToken(String token, long expirationMs) {
        redisTemplate.opsForValue().set(
                BLACKLISTED_TOKEN_PREFIX + token,
                "blacklisted",
                expirationMs,
                TimeUnit.MILLISECONDS
        );
        log.debug("Blacklisted token with expiration: {}", expirationMs);
    }

    /**
     * Remove tokens for a user (logout)
     */
    public void removeRefreshToken(Long userId) {
        String tokenKey = REFRESH_TOKEN_PREFIX + userId;
        String accessTokenKey = ACCESS_TOKEN_PREFIX + userId;

        // Get tokens before deleting
        String refreshToken = redisTemplate.opsForValue().get(tokenKey);
        String accessToken = redisTemplate.opsForValue().get(accessTokenKey);

        // Blacklist the tokens
        if (refreshToken != null) {
            blacklistToken(refreshToken, MAX_TOKEN_LIFETIME * 1000);
            // Remove remember me marker if exists
            redisTemplate.delete(REMEMBER_ME_PREFIX + refreshToken);
        }

        if (accessToken != null) {
            blacklistToken(accessToken, MAX_TOKEN_LIFETIME * 1000);
        }

        // Delete the tokens
        redisTemplate.delete(tokenKey);
        redisTemplate.delete(accessTokenKey);

        log.debug("Removed tokens for user: {}", userId);
    }

    /**
     * Revoke all tokens for a user
     */
    public void revokeAllUserTokens(Long userId) {
        String userKey = USER_TOKENS_PREFIX + userId;

        // Get all token keys for the user
        var tokenKeys = redisTemplate.opsForSet().members(userKey);
        if (tokenKeys != null) {
            for (String tokenKey : tokenKeys) {
                String token = redisTemplate.opsForValue().get(tokenKey);
                if (token != null) {
                    // Blacklist the token
                    blacklistToken(token, MAX_TOKEN_LIFETIME * 1000);

                    // Remove remember me marker if exists
                    if (tokenKey.startsWith(REFRESH_TOKEN_PREFIX)) {
                        redisTemplate.delete(REMEMBER_ME_PREFIX + token);
                    }
                }

                // Delete the token
                redisTemplate.delete(tokenKey);
            }
        }

        // Clear the user's token set
        redisTemplate.delete(userKey);

        log.info("Revoked all tokens for user: {}", userId);
    }

    /**
     * Record a failed login attempt
     * @return true if account should be locked
     */
    public boolean recordFailedLogin(String usernameOrEmail) {
        String key = FAILED_LOGIN_PREFIX + usernameOrEmail;
        Long attempts = redisTemplate.opsForValue().increment(key);

        // Set key expiration if first failure
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(key, 1, TimeUnit.HOURS);
        }

        // Check if account should be locked
        if (attempts != null && attempts >= MAX_FAILED_ATTEMPTS) {
            lockUserAccount(usernameOrEmail);
            return true;
        }

        return false;
    }

    /**
     * Clear failed login attempts
     */
    public void clearFailedLogins(String usernameOrEmail) {
        redisTemplate.delete(FAILED_LOGIN_PREFIX + usernameOrEmail);
    }

    /**
     * Lock a user account temporarily
     */
    private void lockUserAccount(String usernameOrEmail) {
        String key = USER_LOCKOUT_PREFIX + usernameOrEmail;
        redisTemplate.opsForValue().set(key, "locked", LOCKOUT_DURATION_MINUTES, TimeUnit.MINUTES);
        log.warn("Account locked due to multiple failed attempts: {}", usernameOrEmail);
    }

    /**
     * Check if a user account is locked
     */
    public boolean isUserLocked(String usernameOrEmail) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(USER_LOCKOUT_PREFIX + usernameOrEmail));
    }

    /**
     * Get remaining lockout time in seconds
     */
    public Long getLockoutTimeRemaining(String usernameOrEmail) {
        String key = USER_LOCKOUT_PREFIX + usernameOrEmail;
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * Unlock a user account
     */
    public void unlockUserAccount(String usernameOrEmail) {
        redisTemplate.delete(USER_LOCKOUT_PREFIX + usernameOrEmail);
        redisTemplate.delete(FAILED_LOGIN_PREFIX + usernameOrEmail);
        log.info("Account unlocked: {}", usernameOrEmail);
    }

    /**
     * Get token absolute age across refreshes by checking lineage
     */
    public boolean isTokenFamilyExpired(String tokenId) {
        String key = TOKEN_FAMILY_PREFIX + tokenId;
        Long creationTime = redisTemplate.opsForValue().increment(key, 0);

        if (creationTime == null) {
            // Token family not found, create it
            redisTemplate.opsForValue().set(key, String.valueOf(System.currentTimeMillis()), MAX_TOKEN_LIFETIME, TimeUnit.SECONDS);
            return false;
        }

        // Check if creation time is older than MAX_TOKEN_LIFETIME
        return System.currentTimeMillis() - creationTime > (MAX_TOKEN_LIFETIME * 1000);
    }

    /**
     * Rotate a refresh token by invalidating the old one and creating a family link
     */
    public void rotateRefreshToken(Long userId, String oldToken, String newToken, String tokenId) {
        rotateRefreshToken(userId, oldToken, newToken, tokenId, MAX_TOKEN_LIFETIME * 1000);
    }

    /**
     * Rotate a refresh token with custom expiration
     */
    public void rotateRefreshToken(Long userId, String oldToken, String newToken, String tokenId, long expirationMs) {
        // Blacklist old token
        blacklistToken(oldToken, expirationMs);

        // Check if this was a remember me token
        boolean isRememberMe = isRememberMeToken(oldToken);

        // If it was a remember me token, remove the marker but create one for the new token
        if (isRememberMe) {
            redisTemplate.delete(REMEMBER_ME_PREFIX + oldToken);
            redisTemplate.opsForValue().set(
                    REMEMBER_ME_PREFIX + newToken,
                    "true",
                    expirationMs,
                    TimeUnit.MILLISECONDS
            );
        }

        // Save new token
        saveRefreshToken(userId, newToken, expirationMs);

        // Update token family
        String key = TOKEN_FAMILY_PREFIX + tokenId;
        if (!redisTemplate.hasKey(key)) {
            // Create new token family
            redisTemplate.opsForValue().set(key, String.valueOf(System.currentTimeMillis()), MAX_TOKEN_LIFETIME, TimeUnit.SECONDS);
        }

        log.debug("Rotated refresh token for user: {}", userId);
    }
}