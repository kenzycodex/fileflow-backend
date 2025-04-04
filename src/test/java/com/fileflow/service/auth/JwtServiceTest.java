package com.fileflow.service.auth;

import com.fileflow.config.JwtConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JwtServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private JwtConfig jwtConfig;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private JwtService jwtService;

    private final Long USER_ID = 1L;
    private final String ACCESS_TOKEN = "sample-access-token";
    private final String REFRESH_TOKEN = "sample-refresh-token";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(jwtConfig.getExpiration()).thenReturn(3600000L);
        when(jwtConfig.getRefreshExpiration()).thenReturn(604800000L);
    }

    @Test
    void saveAccessToken_shouldSaveTokenWithExpiration() {
        // When
        jwtService.saveAccessToken(USER_ID, ACCESS_TOKEN);

        // Then
        verify(valueOperations).set(eq("token:access:1"), eq(ACCESS_TOKEN), eq(3600000L), eq(TimeUnit.MILLISECONDS));
        verify(redisTemplate).opsForSet().add(eq("user:tokens:1"), eq("token:access:1"));
    }

    @Test
    void saveRefreshToken_shouldSaveTokenWithExpiration() {
        // When
        jwtService.saveRefreshToken(USER_ID, REFRESH_TOKEN);

        // Then
        verify(valueOperations).set(eq("token:refresh:1"), eq(REFRESH_TOKEN), eq(604800000L), eq(TimeUnit.MILLISECONDS));
        verify(redisTemplate).opsForSet().add(eq("user:tokens:1"), eq("token:refresh:1"));
    }

    @Test
    void getLatestAccessToken_shouldReturnToken() {
        // Given
        when(valueOperations.get("token:access:1")).thenReturn(ACCESS_TOKEN);

        // When
        String token = jwtService.getLatestAccessToken(USER_ID);

        // Then
        assertEquals(ACCESS_TOKEN, token);
        verify(valueOperations).get("token:access:1");
    }

    @Test
    void validateRefreshToken_shouldReturnTrueWhenValid() {
        // Given
        when(valueOperations.get("token:refresh:1")).thenReturn(REFRESH_TOKEN);

        // When
        boolean isValid = jwtService.validateRefreshToken(USER_ID, REFRESH_TOKEN);

        // Then
        assertTrue(isValid);
        verify(valueOperations).get("token:refresh:1");
    }

    @Test
    void validateRefreshToken_shouldReturnFalseWhenInvalid() {
        // Given
        when(valueOperations.get("token:refresh:1")).thenReturn("different-token");

        // When
        boolean isValid = jwtService.validateRefreshToken(USER_ID, REFRESH_TOKEN);

        // Then
        assertFalse(isValid);
        verify(valueOperations).get("token:refresh:1");
    }

    @Test
    void isTokenBlacklisted_shouldReturnTrueWhenBlacklisted() {
        // Given
        when(redisTemplate.hasKey("token:blacklisted:" + ACCESS_TOKEN)).thenReturn(true);

        // When
        boolean isBlacklisted = jwtService.isTokenBlacklisted(ACCESS_TOKEN);

        // Then
        assertTrue(isBlacklisted);
        verify(redisTemplate).hasKey("token:blacklisted:" + ACCESS_TOKEN);
    }

    @Test
    void blacklistToken_shouldBlacklistToken() {
        // When
        jwtService.blacklistToken(ACCESS_TOKEN, 3600000);

        // Then
        verify(valueOperations).set(
                eq("token:blacklisted:" + ACCESS_TOKEN),
                eq("blacklisted"),
                eq(3600000L),
                eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    void removeRefreshToken_shouldRemoveTokens() {
        // Given
        when(valueOperations.get("token:refresh:1")).thenReturn(REFRESH_TOKEN);
        when(valueOperations.get("token:access:1")).thenReturn(ACCESS_TOKEN);

        // When
        jwtService.removeRefreshToken(USER_ID);

        // Then
        verify(redisTemplate).delete("token:refresh:1");
        verify(redisTemplate).delete("token:access:1");
        verify(valueOperations).set(
                eq("token:blacklisted:" + REFRESH_TOKEN),
                eq("blacklisted"),
                eq(604800000L),
                eq(TimeUnit.MILLISECONDS)
        );
        verify(valueOperations).set(
                eq("token:blacklisted:" + ACCESS_TOKEN),
                eq("blacklisted"),
                eq(3600000L),
                eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    void recordFailedLogin_shouldIncrementCounter() {
        // Given
        String username = "testuser";
        when(valueOperations.increment("login:failed:" + username)).thenReturn(1L);

        // When
        boolean shouldLock = jwtService.recordFailedLogin(username);

        // Then
        assertFalse(shouldLock);
        verify(valueOperations).increment("login:failed:" + username);
        verify(redisTemplate).expire(eq("login:failed:" + username), eq(1L), eq(TimeUnit.HOURS));
    }

    @Test
    void recordFailedLogin_shouldLockAccountAfterMaxAttempts() {
        // Given
        String username = "testuser";
        when(valueOperations.increment("login:failed:" + username)).thenReturn(5L);

        // When
        boolean shouldLock = jwtService.recordFailedLogin(username);

        // Then
        assertTrue(shouldLock);
        verify(valueOperations).increment("login:failed:" + username);
        verify(redisTemplate).expire(eq("login:failed:" + username), eq(1L), eq(TimeUnit.HOURS));
        verify(valueOperations).set(
                eq("user:lockout:" + username),
                eq("locked"),
                eq(15L),
                eq(TimeUnit.MINUTES)
        );
    }
}