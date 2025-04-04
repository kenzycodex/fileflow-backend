package com.fileflow.service.auth;

import com.fileflow.dto.request.auth.PasswordResetRequest;
import com.fileflow.dto.request.auth.RefreshTokenRequest;
import com.fileflow.dto.request.auth.SignInRequest;
import com.fileflow.dto.request.auth.SignUpRequest;
import com.fileflow.dto.response.auth.JwtResponse;
import com.fileflow.dto.response.auth.UserResponse;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.model.User;

import java.util.Optional;

/**
 * Service for authentication operations
 */
public interface AuthService {

    /**
     * Authenticate user and return JWT tokens
     */
    JwtResponse signIn(SignInRequest signInRequest);

    /**
     * Register a new user
     */
    ApiResponse signUp(SignUpRequest signUpRequest);

    /**
     * Refresh JWT token using refresh token
     */
    JwtResponse refreshToken(RefreshTokenRequest refreshTokenRequest);

    /**
     * Request password reset
     */
    ApiResponse forgotPassword(String email);

    /**
     * Reset password with token
     */
    ApiResponse resetPassword(String token, PasswordResetRequest request);

    /**
     * Validate a JWT token
     */
    ApiResponse validateToken(String token);

    /**
     * Find user by email
     */
    Optional<User> findUserByEmail(String email);

    /**
     * Update Firebase UID for a user
     */
    void updateFirebaseUid(Long userId, String firebaseUid, String provider);

    /**
     * Authenticate a user with Firebase
     */
    UserResponse authenticateFirebaseUser(Long userId);

    /**
     * Create a new user from Firebase authentication
     */
    UserResponse createFirebaseUser(String firebaseUid, String email, String username,
                                    String firstName, String lastName, String profileImageUrl,
                                    String provider);

    /**
     * Logout a user
     */
    ApiResponse logout(String refreshToken);

    /**
     * Verify email address
     */
    ApiResponse verifyEmail(String token);

    /**
     * Resend verification email
     */
    ApiResponse resendVerificationEmail(String email);
}