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

public interface AuthService {
    JwtResponse signIn(SignInRequest signInRequest);

    ApiResponse signUp(SignUpRequest signUpRequest);

    JwtResponse refreshToken(RefreshTokenRequest refreshTokenRequest);

    ApiResponse forgotPassword(String email);

    ApiResponse resetPassword(String token, PasswordResetRequest request);

    ApiResponse validateToken(String token);

    // New methods for Firebase authentication
    Optional<User> findUserByEmail(String email);

    void updateFirebaseUid(Long userId, String firebaseUid, String provider);

    UserResponse authenticateFirebaseUser(Long userId);

    UserResponse createFirebaseUser(String firebaseUid, String email, String username,
                                    String firstName, String lastName, String profileImageUrl,
                                    String provider);

    // New method for logout
    ApiResponse logout(String refreshToken);
}