package com.fileflow.service.auth;

import com.fileflow.config.JwtConfig;
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
    private final JwtConfig jwtConfig;

    // Redis key prefixes
    private static final String ACCESS_TOKEN_PREFIX = "token:access:";
    private static final String REFRESH_TOKEN_PREFIX = "token:refresh:";
    private static final String USER_TOKENS_PREFIX = "user:tokens:";
    private static final String BLACKLISTED_TOKEN_PREFIX = "token:blacklisted:";
    private static final String FAILED_LOGIN_PREFIX = "login:failed:";
    private static final String USER_LOCKOUT_PREFIX = "user:lockout:";

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
        String userKey = USER_TOKENS_PREFIX + userId;
        String tokenKey = ACCESS_TOKEN_PREFIX + userId;

        // Store token with expiration
        redisTemplate.opsForValue().set(
                tokenKey,
                accessToken,
                jwtConfig.getExpiration(),
                TimeUnit.MILLISECONDS
        );

        // Add to user's token collection
        redisTemplate.opsForSet().add(userKey, tokenKey);

        log.debug("Saved access token for user: {}", userId);
    }

    /**
     * Save refresh token for a user
     */
    public void saveRefreshToken(Long userId, String refreshToken) {
        String userKey = USER_TOKENS_PREFIX + userId;
        String tokenKey = REFRESH_TOKEN_PREFIX + userId;

        // Store token with expiration
        redisTemplate.opsForValue().set(
                tokenKey,
                refreshToken,
                jwtConfig.getRefreshExpiration(),
                TimeUnit.MILLISECONDS
        );

        // Add to user's token collection
        redisTemplate.opsForSet().add(userKey, tokenKey);

        log.debug("Saved refresh token for user: {}", userId);
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
            blacklistToken(refreshToken, jwtConfig.getRefreshExpiration());
        }

        if (accessToken != null) {
            blacklistToken(accessToken, jwtConfig.getExpiration());
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
                    // Determine if it's an access or refresh token
                    long expiration = tokenKey.startsWith(ACCESS_TOKEN_PREFIX) ?
                            jwtConfig.getExpiration() : jwtConfig.getRefreshExpiration();

                    // Blacklist the token
                    blacklistToken(token, expiration);
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
        String key = "token:family:" + tokenId;
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
        // Blacklist old token
        blacklistToken(oldToken, jwtConfig.getRefreshExpiration());

        // Save new token
        saveRefreshToken(userId, newToken);

        // Update token family
        String key = "token:family:" + tokenId;
        if (!redisTemplate.hasKey(key)) {
            // Create new token family
            redisTemplate.opsForValue().set(key, String.valueOf(System.currentTimeMillis()), MAX_TOKEN_LIFETIME, TimeUnit.SECONDS);
        }

        log.debug("Rotated refresh token for user: {}", userId);
    }
}