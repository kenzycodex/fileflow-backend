package com.fileflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fileflow.config.TestSecurityConfig;
import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.security.JwtAuthenticationFilter;
import com.fileflow.service.statistics.StatisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatisticsController.class)
@Import(TestSecurityConfig.class)
public class StatisticsControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockBean
    private StatisticsService statisticsService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser
    public void testGetStorageStatistics() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("totalSpace", 10737418240L); // 10GB
        data.put("usedSpace", 2147483648L); // 2GB
        data.put("availableSpace", 8589934592L); // 8GB
        data.put("usagePercentage", 20.0);
        data.put("fileCount", 56);
        data.put("folderCount", 12);
        data.put("averageFileSize", 38345423L); // ~36.5MB

        ApiResponse response = ApiResponse.builder()
                .success(true)
                .message("Storage statistics retrieved successfully")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();

        when(statisticsService.getStorageStatistics()).thenReturn(response);

        mockMvc.perform(get("/api/v1/statistics/storage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Storage statistics retrieved successfully"))
                .andExpect(jsonPath("$.data.totalSpace").value(10737418240L))
                .andExpect(jsonPath("$.data.usedSpace").value(2147483648L))
                .andExpect(jsonPath("$.data.availableSpace").value(8589934592L))
                .andExpect(jsonPath("$.data.usagePercentage").value(20.0))
                .andExpect(jsonPath("$.data.fileCount").value(56))
                .andExpect(jsonPath("$.data.folderCount").value(12))
                .andExpect(jsonPath("$.data.averageFileSize").value(38345423L));
    }

    @Test
    @WithMockUser
    public void testGetActivityStatistics() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("uploads", 25);
        data.put("downloads", 42);
        data.put("shares", 10);
        data.put("lastUpload", LocalDateTime.now().minusHours(2).toString());
        data.put("lastDownload", LocalDateTime.now().minusHours(1).toString());
        data.put("mostActiveDay", "MONDAY");
        data.put("totalActions", 77);

        ApiResponse response = ApiResponse.builder()
                .success(true)
                .message("Activity statistics retrieved successfully")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();

        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        when(statisticsService.getActivityStatistics(eq(startDate), eq(endDate))).thenReturn(response);

        mockMvc.perform(get("/api/v1/statistics/activity")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Activity statistics retrieved successfully"))
                .andExpect(jsonPath("$.data.uploads").value(25))
                .andExpect(jsonPath("$.data.downloads").value(42))
                .andExpect(jsonPath("$.data.shares").value(10))
                .andExpect(jsonPath("$.data.mostActiveDay").value("MONDAY"))
                .andExpect(jsonPath("$.data.totalActions").value(77));
    }

    @Test
    @WithMockUser
    public void testGetFileTypeStatistics() throws Exception {
        Map<String, Object> data = new HashMap<>();
        Map<String, Integer> fileTypes = new HashMap<>();
        fileTypes.put("document", 25);
        fileTypes.put("image", 30);
        fileTypes.put("video", 5);
        fileTypes.put("audio", 8);
        fileTypes.put("other", 7);
        data.put("fileTypes", fileTypes);
        data.put("totalFiles", 75);
        data.put("mostCommonType", "image");

        ApiResponse response = ApiResponse.builder()
                .success(true)
                .message("File type statistics retrieved successfully")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();

        when(statisticsService.getFileTypeStatistics()).thenReturn(response);

        mockMvc.perform(get("/api/v1/statistics/file-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("File type statistics retrieved successfully"))
                .andExpect(jsonPath("$.data.fileTypes.document").value(25))
                .andExpect(jsonPath("$.data.fileTypes.image").value(30))
                .andExpect(jsonPath("$.data.fileTypes.video").value(5))
                .andExpect(jsonPath("$.data.fileTypes.audio").value(8))
                .andExpect(jsonPath("$.data.fileTypes.other").value(7))
                .andExpect(jsonPath("$.data.totalFiles").value(75))
                .andExpect(jsonPath("$.data.mostCommonType").value("image"));
    }

    @Test
    @WithMockUser
    public void testGetSharingStatistics() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("totalShares", 36);
        data.put("activeShares", 28);
        data.put("expiredShares", 8);
        data.put("passwordProtectedShares", 15);
        data.put("publicShares", 21);
        data.put("mostSharedFileType", "document");
        data.put("averageShareDuration", "7 days");

        ApiResponse response = ApiResponse.builder()
                .success(true)
                .message("Sharing statistics retrieved successfully")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();

        when(statisticsService.getSharingStatistics()).thenReturn(response);

        mockMvc.perform(get("/api/v1/statistics/sharing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Sharing statistics retrieved successfully"))
                .andExpect(jsonPath("$.data.totalShares").value(36))
                .andExpect(jsonPath("$.data.activeShares").value(28))
                .andExpect(jsonPath("$.data.expiredShares").value(8))
                .andExpect(jsonPath("$.data.passwordProtectedShares").value(15))
                .andExpect(jsonPath("$.data.publicShares").value(21))
                .andExpect(jsonPath("$.data.mostSharedFileType").value("document"))
                .andExpect(jsonPath("$.data.averageShareDuration").value("7 days"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testGetSystemStatistics() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("totalUsers", 250);
        data.put("activeUsers", 180);
        data.put("totalFiles", 5678);
        data.put("totalStorage", "500GB");
        data.put("averageUserStorage", "2GB");
        data.put("systemUptime", "30 days");
        data.put("newUsersLast30Days", 45);

        ApiResponse response = ApiResponse.builder()
                .success(true)
                .message("System statistics retrieved successfully")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();

        when(statisticsService.getSystemStatistics()).thenReturn(response);

        mockMvc.perform(get("/api/v1/statistics/system"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("System statistics retrieved successfully"))
                .andExpect(jsonPath("$.data.totalUsers").value(250))
                .andExpect(jsonPath("$.data.activeUsers").value(180))
                .andExpect(jsonPath("$.data.totalFiles").value(5678))
                .andExpect(jsonPath("$.data.totalStorage").value("500GB"))
                .andExpect(jsonPath("$.data.averageUserStorage").value("2GB"))
                .andExpect(jsonPath("$.data.systemUptime").value("30 days"))
                .andExpect(jsonPath("$.data.newUsersLast30Days").value(45));
    }
}