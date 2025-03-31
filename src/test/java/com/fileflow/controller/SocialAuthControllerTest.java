package com.fileflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fileflow.dto.request.auth.FirebaseAuthRequest;
import com.fileflow.dto.response.auth.JwtResponse;
import com.fileflow.dto.response.auth.UserResponse;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.security.JwtTokenProvider;
import com.fileflow.service.auth.AuthService;
import com.fileflow.service.auth.FirebaseAuthService;
import com.fileflow.service.auth.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SocialAuthController.class)
public class SocialAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FirebaseAuthService firebaseAuthService;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider tokenProvider;

    @MockBean
    private JwtService jwtService;

    @Test
    public void testAuthenticateWithFirebase() throws Exception {
        // Mock data
        FirebaseAuthRequest request = new FirebaseAuthRequest();
        request.setIdToken("test-firebase-token");

        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .authProvider("GOOGLE")
                .build();

        // Mock service responses
        when(firebaseAuthService.authenticateWithFirebase(anyString())).thenReturn(userResponse);
        when(tokenProvider.generateToken(any(Long.class))).thenReturn("test-access-token");
        when(tokenProvider.generateRefreshToken(any(Long.class))).thenReturn("test-refresh-token");

        // Perform request
        mockMvc.perform(post("/api/v1/auth/social/firebase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("test-refresh-token"))
                .andExpect(jsonPath("$.user.id").value(1))
                .andExpect(jsonPath("$.user.username").value("testuser"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.user.authProvider").value("GOOGLE"));
    }

    @Test
    public void testLogout() throws Exception {
        // Mock response
        ApiResponse apiResponse = ApiResponse.builder()
                .success(true)
                .message("Logged out successfully")
                .timestamp(LocalDateTime.now())
                .build();

        when(authService.logout(anyString())).thenReturn(apiResponse);

        // Perform request
        mockMvc.perform(post("/api/v1/auth/social/logout")
                        .param("refreshToken", "test-refresh-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }
}