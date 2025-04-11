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
 * Authentication service interface
 */
public interface AuthService {

    /**
     * Sign in user
     * @param signInRequest Sign in request DTO
     * @return JWT response
     */
    JwtResponse signIn(SignInRequest signInRequest);

    /**
     * Register a new user
     * @param signUpRequest Sign up request DTO
     * @return API response
     */
    ApiResponse signUp(SignUpRequest signUpRequest);

    /**
     * Refresh authentication token
     * @param refreshTokenRequest Refresh token request DTO
     * @return JWT response
     */
    JwtResponse refreshToken(RefreshTokenRequest refreshTokenRequest);

    /**
     * Request password reset
     * @param email User email
     * @return API response
     */
    ApiResponse forgotPassword(String email);

    /**
     * Reset password with token
     * @param token Reset token
     * @param request Password reset request DTO
     * @return API response
     */
    ApiResponse resetPassword(String token, PasswordResetRequest request);

    /**
     * Validate JWT token
     * @param token JWT token
     * @return API response
     */
    ApiResponse validateToken(String token);

    /**
     * Validate password reset token
     * @param token Reset token
     * @return API response
     */
    ApiResponse validateResetToken(String token);

    /**
     * Validate email verification token
     * @param token Verification token
     * @return API response
     */
    ApiResponse validateVerificationToken(String token);

    /**
     * Find user by email
     * @param email Email address
     * @return Optional user
     */
    Optional<User> findUserByEmail(String email);

    /**
     * Update Firebase UID
     * @param userId User ID
     * @param firebaseUid Firebase UID
     * @param provider Auth provider
     */
    void updateFirebaseUid(Long userId, String firebaseUid, String provider);

    /**
     * Authenticate Firebase user
     * @param userId User ID
     * @return User response
     */
    UserResponse authenticateFirebaseUser(Long userId);

    /**
     * Create Firebase user
     * @param firebaseUid Firebase UID
     * @param email Email
     * @param username Username
     * @param firstName First name
     * @param lastName Last name
     * @param profileImageUrl Profile image URL
     * @param provider Auth provider
     * @return User response
     */
    UserResponse createFirebaseUser(String firebaseUid, String email, String username,
                                    String firstName, String lastName, String profileImageUrl,
                                    String provider);

    /**
     * Logout user
     * @param refreshToken Refresh token
     * @return API response
     */
    ApiResponse logout(String refreshToken);

    /**
     * Verify email
     * @param token Verification token
     * @return API response
     */
    ApiResponse verifyEmail(String token);

    /**
     * Resend verification email
     * @param email User email
     * @return API response
     */
    ApiResponse resendVerificationEmail(String email);
}