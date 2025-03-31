package com.fileflow.service.auth;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to manage JWT tokens.
 * In a production environment, this should use Redis or a database to persist tokens.
 */
@Service
public class JwtService {
    private final Map<Long, String> refreshTokenStore = new ConcurrentHashMap<>();
    private final Map<Long, String> accessTokenStore = new ConcurrentHashMap<>(); // Store latest access tokens

    /**
     * Save refresh token for a user
     */
    public void saveRefreshToken(Long userId, String refreshToken) {
        refreshTokenStore.put(userId, refreshToken);
    }

    /**
     * Save access token for a user
     */
    public void saveAccessToken(Long userId, String accessToken) {
        accessTokenStore.put(userId, accessToken);
    }

    /**
     * Get the latest access token for a user
     */
    public String getLatestAccessToken(Long userId) {
        return accessTokenStore.get(userId);
    }

    /**
     * Validate if the refresh token exists and matches for the user
     */
    public boolean validateRefreshToken(Long userId, String refreshToken) {
        String storedToken = refreshTokenStore.get(userId);
        return storedToken != null && storedToken.equals(refreshToken);
    }

    /**
     * Remove tokens for a user (logout)
     */
    public void removeRefreshToken(Long userId) {
        refreshTokenStore.remove(userId);
        accessTokenStore.remove(userId);
    }
}