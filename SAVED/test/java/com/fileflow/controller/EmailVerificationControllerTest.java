package com.fileflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fileflow.config.TestSecurityConfig;
import com.fileflow.dto.request.auth.EmailVerificationRequest;
import com.fileflow.dto.request.auth.ResendVerificationRequest;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.service.auth.AuthService;
import com.fileflow.service.security.RateLimiterService;
import com.fileflow.util.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmailVerificationController.class)
@Import(TestSecurityConfig.class)
public class EmailVerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private RateLimiterService rateLimiterService;

    @MockBean
    private SecurityUtils securityUtils;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testVerifyEmailWithToken() throws Exception {
        // Setup mocks
        String token = "valid-verification-token";
        ApiResponse apiResponse = ApiResponse.builder()
                .success(true)
                .message("Email verified successfully")
                .timestamp(LocalDateTime.now())
                .build();

        when(securityUtils.getClientIpAddress(org.mockito.ArgumentMatchers.any())).thenReturn("127.0.0.1");
        doNothing().when(rateLimiterService).checkRateLimit(anyString());
        when(authService.verifyEmail(eq(token))).thenReturn(apiResponse);

        // Perform the request
        mockMvc.perform(get("/api/v1/auth/verify")
                        .param("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Email verified successfully"));
    }

    @Test
    public void testVerifyEmailPost() throws Exception {
        // Setup request data
        EmailVerificationRequest request = new EmailVerificationRequest();
        request.setToken("valid-verification-token");

        // Setup mocks
        ApiResponse apiResponse = ApiResponse.builder()
                .success(true)
                .message("Email verified successfully")
                .timestamp(LocalDateTime.now())
                .build();

        when(securityUtils.getClientIpAddress(org.mockito.ArgumentMatchers.any())).thenReturn("127.0.0.1");
        doNothing().when(rateLimiterService).checkRateLimit(anyString());
        when(authService.verifyEmail(eq("valid-verification-token"))).thenReturn(apiResponse);

        // Perform the request
        mockMvc.perform(post("/api/v1/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Email verified successfully"));
    }

    @Test
    public void testResendVerificationEmail() throws Exception {
        // Setup request data
        ResendVerificationRequest request = new ResendVerificationRequest();
        request.setEmail("test@example.com");

        // Setup mocks
        ApiResponse apiResponse = ApiResponse.builder()
                .success(true)
                .message("Verification email sent successfully")
                .timestamp(LocalDateTime.now())
                .build();

        when(securityUtils.getClientIpAddress(org.mockito.ArgumentMatchers.any())).thenReturn("127.0.0.1");
        doNothing().when(rateLimiterService).checkRateLimit(anyString());
        when(authService.resendVerificationEmail(eq("test@example.com"))).thenReturn(apiResponse);

        // Perform the request
        mockMvc.perform(post("/api/v1/auth/verify/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Verification email sent successfully"));
    }
}