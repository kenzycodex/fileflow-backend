package com.fileflow.service.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    private final Long userId = 1L;
    private final String refreshToken = "sample-refresh-token";
    private final String accessToken = "sample-access-token";

    @BeforeEach
    public void setup() {
        // Reset any state between tests if needed
    }

    @Test
    public void testSaveAndValidateRefreshToken() {
        // Save a refresh token
        jwtService.saveRefreshToken(userId, refreshToken);

        // Validate the saved token
        boolean isValid = jwtService.validateRefreshToken(userId, refreshToken);

        // Assert
        assertTrue(isValid, "Refresh token should be valid");
    }

    @Test
    public void testValidateInvalidRefreshToken() {
        // Save a refresh token
        jwtService.saveRefreshToken(userId, refreshToken);

        // Validate with wrong token
        boolean isValidWrongToken = jwtService.validateRefreshToken(userId, "wrong-token");

        // Validate with wrong user ID
        boolean isValidWrongUser = jwtService.validateRefreshToken(2L, refreshToken);

        // Assert
        assertFalse(isValidWrongToken, "Wrong token should be invalid");
        assertFalse(isValidWrongUser, "Token for wrong user should be invalid");
    }

    @Test
    public void testRemoveRefreshToken() {
        // Save a refresh token
        jwtService.saveRefreshToken(userId, refreshToken);

        // Remove the token
        jwtService.removeRefreshToken(userId);

        // Validate the token after removal
        boolean isValid = jwtService.validateRefreshToken(userId, refreshToken);

        // Assert
        assertFalse(isValid, "Token should be invalid after removal");
    }

    @Test
    public void testOverwriteRefreshToken() {
        // Save an initial token
        jwtService.saveRefreshToken(userId, "initial-token");

        // Save a new token for the same user
        jwtService.saveRefreshToken(userId, refreshToken);

        // Validate both tokens
        boolean isInitialValid = jwtService.validateRefreshToken(userId, "initial-token");
        boolean isNewValid = jwtService.validateRefreshToken(userId, refreshToken);

        // Assert
        assertFalse(isInitialValid, "Initial token should be invalid after overwriting");
        assertTrue(isNewValid, "New token should be valid");
    }

    @Test
    public void testSaveAndGetAccessToken() {
        // Save an access token
        jwtService.saveAccessToken(userId, accessToken);

        // Get the latest access token
        String savedToken = jwtService.getLatestAccessToken(userId);

        // Assert
        assertEquals(accessToken, savedToken, "Retrieved access token should match the saved one");
    }

    @Test
    public void testLogoutRemovesBothTokens() {
        // Save both token types
        jwtService.saveAccessToken(userId, accessToken);
        jwtService.saveRefreshToken(userId, refreshToken);

        // Verify tokens are saved
        assertNotNull(jwtService.getLatestAccessToken(userId));
        assertTrue(jwtService.validateRefreshToken(userId, refreshToken));

        // Remove tokens (logout)
        jwtService.removeRefreshToken(userId);

        // Verify both tokens are removed
        assertNull(jwtService.getLatestAccessToken(userId));
        assertFalse(jwtService.validateRefreshToken(userId, refreshToken));
    }
}