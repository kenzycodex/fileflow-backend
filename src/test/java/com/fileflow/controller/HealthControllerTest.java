package com.fileflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fileflow.config.TestSecurityConfig;
import com.fileflow.dto.response.health.HealthCheckResponse;
import com.fileflow.security.JwtAuthenticationFilter;
import com.fileflow.service.health.HealthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
@Import(TestSecurityConfig.class)
public class HealthControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockBean
    private HealthService healthService;

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
    public void testCheckHealth_AllUp() throws Exception {
        Map<String, Map<String, Object>> components = new HashMap<>();
        components.put("database", Map.of("status", "UP", "responseTime", 15));
        components.put("storage", Map.of("status", "UP", "responseTime", 25));
        components.put("email", Map.of("status", "UP", "responseTime", 100));

        HealthCheckResponse response = HealthCheckResponse.builder()
                .status("UP")
                .components(components)
                .build();

        when(healthService.checkHealth()).thenReturn(response);

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.database.status").value("UP"))
                .andExpect(jsonPath("$.components.storage.status").value("UP"))
                .andExpect(jsonPath("$.components.email.status").value("UP"));
    }

    @Test
    public void testCheckHealth_PartialDown() throws Exception {
        Map<String, Map<String, Object>> components = new HashMap<>();
        components.put("database", Map.of("status", "UP", "responseTime", 15));
        components.put("storage", Map.of("status", "UP", "responseTime", 25));
        components.put("email", Map.of("status", "DOWN", "error", "Connection timeout"));

        HealthCheckResponse response = HealthCheckResponse.builder()
                .status("DOWN")
                .components(components)
                .build();

        when(healthService.checkHealth()).thenReturn(response);

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.components.database.status").value("UP"))
                .andExpect(jsonPath("$.components.storage.status").value("UP"))
                .andExpect(jsonPath("$.components.email.status").value("DOWN"))
                .andExpect(jsonPath("$.components.email.error").value("Connection timeout"));
    }

    @Test
    public void testCheckDatabaseHealth() throws Exception {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("responseTime", 15);
        response.put("activeConnections", 5);
        response.put("maxConnections", 20);

        when(healthService.checkDatabase()).thenReturn(response);

        mockMvc.perform(get("/api/v1/health/database"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.responseTime").value(15))
                .andExpect(jsonPath("$.activeConnections").value(5))
                .andExpect(jsonPath("$.maxConnections").value(20));
    }

    @Test
    public void testCheckStorageHealth() throws Exception {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("responseTime", 25);
        response.put("availableSpace", "500GB");
        response.put("totalSpace", "1TB");

        when(healthService.checkStorage()).thenReturn(response);

        mockMvc.perform(get("/api/v1/health/storage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.responseTime").value(25))
                .andExpect(jsonPath("$.availableSpace").value("500GB"))
                .andExpect(jsonPath("$.totalSpace").value("1TB"));
    }

    @Test
    public void testCheckEmailHealth() throws Exception {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("responseTime", 100);
        response.put("provider", "SMTP");

        when(healthService.checkEmail()).thenReturn(response);

        mockMvc.perform(get("/api/v1/health/email"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.responseTime").value(100))
                .andExpect(jsonPath("$.provider").value("SMTP"));
    }

    @Test
    public void testGetSystemMetrics() throws Exception {
        Map<String, Object> response = new HashMap<>();
        response.put("cpu", Map.of("usage", "45%", "cores", 8));
        response.put("memory", Map.of("used", "4.5GB", "total", "16GB"));
        response.put("disk", Map.of("used", "250GB", "total", "1TB"));

        when(healthService.getSystemMetrics()).thenReturn(response);

        mockMvc.perform(get("/api/v1/health/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpu.usage").value("45%"))
                .andExpect(jsonPath("$.cpu.cores").value(8))
                .andExpect(jsonPath("$.memory.used").value("4.5GB"))
                .andExpect(jsonPath("$.memory.total").value("16GB"))
                .andExpect(jsonPath("$.disk.used").value("250GB"))
                .andExpect(jsonPath("$.disk.total").value("1TB"));
    }
}