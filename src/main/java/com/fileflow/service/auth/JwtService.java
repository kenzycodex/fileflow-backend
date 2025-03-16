package com.fileflow.service.auth;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to manage JWT refresh tokens.
 * In a production environment, this should use Redis or a database to persist tokens.
 */
@Service
public class JwtService {
    private final Map<Long, String> refreshTokenStore = new ConcurrentHashMap<>();

    /**
     * Save refresh token for a user
     */
    public void saveRefreshToken(Long userId, String refreshToken) {
        refreshTokenStore.put(userId, refreshToken);
    }

    /**
     * Validate if the refresh token exists and matches for the user
     */
    public boolean validateRefreshToken(Long userId, String refreshToken) {
        String storedToken = refreshTokenStore.get(userId);
        return storedToken != null && storedToken.equals(refreshToken);
    }

    /**
     * Remove refresh token for a user (logout)
     */
    public void removeRefreshToken(Long userId) {
        refreshTokenStore.remove(userId);
    }
}