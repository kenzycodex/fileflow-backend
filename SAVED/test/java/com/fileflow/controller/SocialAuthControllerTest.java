package com.fileflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fileflow.config.TestSecurityConfig;
import com.fileflow.dto.request.auth.FirebaseAuthRequest;
import com.fileflow.dto.response.auth.JwtResponse;
import com.fileflow.dto.response.auth.UserResponse;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.security.JwtTokenProvider;
import com.fileflow.service.auth.AuthService;
import com.fileflow.service.auth.FirebaseAuthService;
import com.fileflow.service.auth.JwtService;
import com.fileflow.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SocialAuthController.class)
@Import(TestSecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false) // Disable Spring Security filters for controller tests
public class SocialAuthControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

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

    @BeforeEach
    public void setUp() {
        // Setup MockMvc with all the required configuration
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .alwaysDo(MockMvcResultHandlers.print()) // Always print for debugging
                .build();

        // Ensure Constants are set for tests
        Constants.ACCESS_TOKEN_EXPIRATION = 3600000L; // 1 hour in milliseconds
    }

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

        // Perform request with detailed debugging
        mockMvc.perform(post("/api/v1/auth/social/firebase")
                        .characterEncoding("UTF-8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print()) // Print request/response details
                .andExpect(status().isOk())
                // Skip specific content type check and check JSON values instead
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

        // Perform request with detailed debugging
        mockMvc.perform(post("/api/v1/auth/social/logout")
                        .characterEncoding("UTF-8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .param("refreshToken", "test-refresh-token"))
                .andDo(print()) // Print request/response details
                .andExpect(status().isOk())
                // Skip specific content type check and check JSON values instead
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }
}