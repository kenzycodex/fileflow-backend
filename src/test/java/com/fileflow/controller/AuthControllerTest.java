package com.fileflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fileflow.config.TestSecurityConfig;
import com.fileflow.dto.request.auth.PasswordResetRequest;
import com.fileflow.dto.request.auth.RefreshTokenRequest;
import com.fileflow.dto.request.auth.SignInRequest;
import com.fileflow.dto.request.auth.SignUpRequest;
import com.fileflow.dto.response.auth.JwtResponse;
import com.fileflow.dto.response.auth.UserResponse;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.security.JwtAuthenticationFilter;
import com.fileflow.service.auth.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
public class AuthControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .build();
    }

    @Test
    public void testSignIn() throws Exception {
        // Create mock data
        SignInRequest signInRequest = SignInRequest.builder()
                .usernameOrEmail("testuser")
                .password("Test@123")
                .build();

        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .storageQuota(10737418240L) // 10GB
                .storageUsed(0L)
                .role("USER")
                .createdAt(LocalDateTime.now())
                .build();

        JwtResponse jwtResponse = JwtResponse.builder()
                .accessToken("test-access-token")
                .refreshToken("test-refresh-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .user(userResponse)
                .build();

        when(authService.signIn(any(SignInRequest.class))).thenReturn(jwtResponse);

        // Perform the sign-in request
        mockMvc.perform(post("/api/v1/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signInRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("test-refresh-token"))
                .andExpect(jsonPath("$.user.username").value("testuser"));
    }

    @Test
    public void testSignUp() throws Exception {
        // Create mock data
        SignUpRequest signUpRequest = SignUpRequest.builder()
                .username("newuser")
                .email("new@example.com")
                .password("NewPass@123")
                .firstName("New")
                .lastName("User")
                .build();

        ApiResponse apiResponse = ApiResponse.builder()
                .success(true)
                .message("User registered successfully")
                .timestamp(LocalDateTime.now())
                .build();

        when(authService.signUp(any(SignUpRequest.class))).thenReturn(apiResponse);

        // Perform the sign-up request
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully"));
    }

    @Test
    public void testRefreshToken() throws Exception {
        // Create mock data
        RefreshTokenRequest refreshTokenRequest = RefreshTokenRequest.builder()
                .refreshToken("old-refresh-token")
                .build();

        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .build();

        JwtResponse jwtResponse = JwtResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .user(userResponse)
                .build();

        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(jwtResponse);

        // Perform the refresh token request
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshTokenRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
    }

    @Test
    public void testForgotPassword() throws Exception {
        // Create mock data
        ApiResponse apiResponse = ApiResponse.builder()
                .success(true)
                .message("Password reset instructions sent to your email")
                .timestamp(LocalDateTime.now())
                .build();

        when(authService.forgotPassword(eq("test@example.com"))).thenReturn(apiResponse);

        // Perform the forgot password request
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .param("email", "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password reset instructions sent to your email"));
    }

    @Test
    public void testResetPassword() throws Exception {
        // Create mock data with token field
        PasswordResetRequest resetRequest = PasswordResetRequest.builder()
                .email("test@example.com")
                .newPassword("NewPass@123")
                .confirmPassword("NewPass@123")
                .token("reset-token")
                .build();

        ApiResponse apiResponse = ApiResponse.builder()
                .success(true)
                .message("Password has been reset successfully")
                .timestamp(LocalDateTime.now())
                .build();

        when(authService.resetPassword(eq("reset-token"), any(PasswordResetRequest.class))).thenReturn(apiResponse);

        // Perform the reset password request
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .param("token", "reset-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password has been reset successfully"));
    }

    @Test
    public void testValidateToken() throws Exception {
        // Create mock data
        ApiResponse apiResponse = ApiResponse.builder()
                .success(true)
                .message("Token is valid")
                .timestamp(LocalDateTime.now())
                .build();

        when(authService.validateToken(eq("valid-token"))).thenReturn(apiResponse);

        // Perform the validate token request
        mockMvc.perform(get("/api/v1/auth/validate-token")
                        .param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Token is valid"));
    }
}