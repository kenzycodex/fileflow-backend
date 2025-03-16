package com.fileflow.service.auth;

import com.fileflow.dto.request.auth.PasswordResetRequest;
import com.fileflow.dto.request.auth.RefreshTokenRequest;
import com.fileflow.dto.request.auth.SignInRequest;
import com.fileflow.dto.request.auth.SignUpRequest;
import com.fileflow.dto.response.auth.JwtResponse;
import com.fileflow.dto.response.common.ApiResponse;

public interface AuthService {
    JwtResponse signIn(SignInRequest signInRequest);

    ApiResponse signUp(SignUpRequest signUpRequest);

    JwtResponse refreshToken(RefreshTokenRequest refreshTokenRequest);

    ApiResponse forgotPassword(String email);

    ApiResponse resetPassword(String token, PasswordResetRequest request);

    ApiResponse validateToken(String token);
}