package com.fileflow.controller;

import com.fileflow.config.TestSecurityConfig;
import com.fileflow.repository.UserRepository;
import com.fileflow.service.security.RateLimiterService;
import com.fileflow.util.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ValidationController.class)
@Import(TestSecurityConfig.class)
public class ValidationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private SecurityUtils securityUtils;

    @MockBean
    private RateLimiterService rateLimiterService;

    @Test
    public void testCheckUsername_available() throws Exception {
        // Setup mocks
        when(securityUtils.getClientIpAddress(org.mockito.ArgumentMatchers.any())).thenReturn("127.0.0.1");
        doNothing().when(rateLimiterService).checkRateLimit(anyString());
        when(securityUtils.sanitizeInput(eq("newuser"))).thenReturn("newuser");
        when(userRepository.existsByUsername(eq("newuser"))).thenReturn(false);

        // Perform the request
        mockMvc.perform(get("/api/v1/users/check-username")
                        .param("username", "newuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Username availability checked"))
                .andExpect(jsonPath("$.data.available").value(true));
    }

    @Test
    public void testCheckUsername_unavailable() throws Exception {
        // Setup mocks
        when(securityUtils.getClientIpAddress(org.mockito.ArgumentMatchers.any())).thenReturn("127.0.0.1");
        doNothing().when(rateLimiterService).checkRateLimit(anyString());
        when(securityUtils.sanitizeInput(eq("existinguser"))).thenReturn("existinguser");
        when(userRepository.existsByUsername(eq("existinguser"))).thenReturn(true);

        // Perform the request
        mockMvc.perform(get("/api/v1/users/check-username")
                        .param("username", "existinguser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Username availability checked"))
                .andExpect(jsonPath("$.data.available").value(false));
    }

    @Test
    public void testCheckEmail_available() throws Exception {
        // Setup mocks
        when(securityUtils.getClientIpAddress(org.mockito.ArgumentMatchers.any())).thenReturn("127.0.0.1");
        doNothing().when(rateLimiterService).checkRateLimit(anyString());
        when(securityUtils.sanitizeInput(eq("new@example.com"))).thenReturn("new@example.com");
        when(userRepository.existsByEmail(eq("new@example.com"))).thenReturn(false);

        // Perform the request
        mockMvc.perform(get("/api/v1/users/check-email")
                        .param("email", "new@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Email availability checked"))
                .andExpect(jsonPath("$.data.available").value(true));
    }

    @Test
    public void testCheckEmail_unavailable() throws Exception {
        // Setup mocks
        when(securityUtils.getClientIpAddress(org.mockito.ArgumentMatchers.any())).thenReturn("127.0.0.1");
        doNothing().when(rateLimiterService).checkRateLimit(anyString());
        when(securityUtils.sanitizeInput(eq("existing@example.com"))).thenReturn("existing@example.com");
        when(userRepository.existsByEmail(eq("existing@example.com"))).thenReturn(true);

        // Perform the request
        mockMvc.perform(get("/api/v1/users/check-email")
                        .param("email", "existing@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Email availability checked"))
                .andExpect(jsonPath("$.data.available").value(false));
    }

    @Test
    public void testCheckUsername_sanitizesInput() throws Exception {
        // Setup mocks for input sanitization
        when(securityUtils.getClientIpAddress(org.mockito.ArgumentMatchers.any())).thenReturn("127.0.0.1");
        doNothing().when(rateLimiterService).checkRateLimit(anyString());
        when(securityUtils.sanitizeInput(eq("<script>alert('xss')</script>newuser"))).thenReturn("newuser");
        when(userRepository.existsByUsername(eq("newuser"))).thenReturn(false);

        // Perform the request with potentially malicious input
        mockMvc.perform(get("/api/v1/users/check-username")
                        .param("username", "<script>alert('xss')</script>newuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.available").value(true));
    }

    @Test
    public void testCheckEmail_sanitizesInput() throws Exception {
        // Setup mocks for input sanitization
        when(securityUtils.getClientIpAddress(org.mockito.ArgumentMatchers.any())).thenReturn("127.0.0.1");
        doNothing().when(rateLimiterService).checkRateLimit(anyString());
        when(securityUtils.sanitizeInput(eq("<script>alert('xss')</script>email@example.com")))
                .thenReturn("email@example.com");
        when(userRepository.existsByEmail(eq("email@example.com"))).thenReturn(false);

        // Perform the request with potentially malicious input
        mockMvc.perform(get("/api/v1/users/check-email")
                        .param("email", "<script>alert('xss')</script>email@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.available").value(true));
    }
}