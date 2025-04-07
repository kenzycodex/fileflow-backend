package com.fileflow.util;

import com.fileflow.dto.request.auth.SignInRequest;
import com.fileflow.dto.request.auth.SignUpRequest;
import com.fileflow.model.User;
import com.fileflow.security.UserPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.Collections;

/**
 * Utilities for creating test objects
 */
public class TestUtils {

    private TestUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Create a test user
     */
    public static User createTestUser() {
        return User.builder()
                .id(1L)
                .firstName("Test")
                .lastName("User")
                .username("testuser")
                .email("test@example.com")
                .password("encoded_password")
                .status(User.Status.ACTIVE)
                .role(User.UserRole.USER)
                .storageQuota(Constants.DEFAULT_STORAGE_QUOTA)
                .storageUsed(0L)
                .createdAt(LocalDateTime.now())
                .enabled(true)
                .emailVerified(true)
                .authProvider(User.AuthProvider.LOCAL)
                .build();
    }

    /**
     * Create a test UserPrincipal
     */
    public static UserPrincipal createTestUserPrincipal() {
        return new UserPrincipal(
                1L,
                "Test",
                "User",
                "testuser",
                "test@example.com",
                "encoded_password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                true
        );
    }

    /**
     * Create a test SignInRequest
     */
    public static SignInRequest createTestSignInRequest() {
        SignInRequest request = new SignInRequest();
        request.setUsernameOrEmail("testuser");
        request.setPassword("password123");
        return request;
    }

    /**
     * Create a test SignUpRequest
     */
    public static SignUpRequest createTestSignUpRequest() {
        SignUpRequest request = new SignUpRequest();
        request.setFirstName("Test");
        request.setLastName("User");
        request.setUsername("newuser");
        request.setEmail("newuser@example.com");
        request.setPassword("Password123!");
        request.setConfirmPassword("Password123!");
        return request;
    }

    /**
     * Generate a test JWT token
     */
    public static String createTestJwtToken() {
        return "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwidHlwZSI6ImFjY2VzcyIsImlhdCI6MTUxNjIzOTAyMn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    }
}