package com.fileflow.service.health;

import com.fileflow.dto.response.health.HealthCheckResponse;
import com.fileflow.repository.UserRepository;
import com.fileflow.service.storage.EnhancedStorageService;
import com.fileflow.service.storage.StorageServiceFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Status;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class HealthServiceImpl implements HealthService {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final StorageServiceFactory storageServiceFactory;
    private final JavaMailSender mailSender;

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${spring.profiles.active}")
    private String activeProfile;

    @Override
    public HealthCheckResponse checkHealth() {
        LocalDateTime timestamp = LocalDateTime.now();
        Map<String, Map<String, Object>> components = new HashMap<>();

        // Check each component
        components.put("database", checkDatabase());
        components.put("storage", checkStorage());
        components.put("email", checkEmail());

        // Determine overall status
        Status overallStatus = components.values().stream()
                .anyMatch(component -> "DOWN".equals(component.get("status")))
                ? Status.DOWN : Status.UP;

        return HealthCheckResponse.builder()
                .status(overallStatus.toString())
                .timestamp(timestamp)
                .application(applicationName)
                .environment(activeProfile)
                .components(components)
                .metrics(getSystemMetrics())
                .build();
    }

    @Override
    public Map<String, Object> checkDatabase() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "DOWN");

        try {
            // Try to execute a simple query
            Integer count = jdbcTemplate.queryForObject("SELECT 1", Integer.class);

            if (count != null && count == 1) {
                status.put("status", "UP");
                status.put("message", "Database is operational");

                // Get user count as an additional check
                long userCount = userRepository.count();
                status.put("userCount", userCount);
            } else {
                status.put("message", "Database query returned unexpected result");
            }
        } catch (Exception e) {
            log.error("Database health check failed", e);
            status.put("status", "DOWN");
            status.put("message", "Error connecting to database: " + e.getMessage());
            status.put("error", e.getClass().getName());
        }

        return status;
    }

    @Override
    public Map<String, Object> checkStorage() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "DOWN");

        try {
            // Get the appropriate storage service
            EnhancedStorageService storageService = storageServiceFactory.getStorageService();

            // Create a test file
            String testFileName = "health-check-" + UUID.randomUUID() + ".txt";
            String testContent = "Health check test file - " + LocalDateTime.now();

            // Create a MultipartFile for the test
            MultipartFile testFile = createMultipartFileFromString(testFileName, testContent);

            // Store test file
            String storagePath = storageService.store(testFile, "health-checks");

            // Check if file exists
            boolean exists = storageService.exists(storagePath);

            if (exists) {
                status.put("status", "UP");
                status.put("message", "Storage service is operational");
                status.put("storagePath", storagePath);

                // Clean up test file
                storageService.delete(storagePath);
            } else {
                status.put("message", "Test file not found after upload");
            }
        } catch (Exception e) {
            log.error("Storage health check failed", e);
            status.put("status", "DOWN");
            status.put("message", "Error with storage service: " + e.getMessage());
            status.put("error", e.getClass().getName());
        }

        return status;
    }

    @Override
    public Map<String, Object> checkEmail() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "DOWN");

        try {
            // Check if mail sender is configured
            mailSender.createMimeMessage();

            // If no exception, mark as UP (don't actually send emails in health check)
            status.put("status", "UP");
            status.put("message", "Email service is configured");
        } catch (Exception e) {
            log.error("Email health check failed", e);
            status.put("status", "DOWN");
            status.put("message", "Error with email service: " + e.getMessage());
            status.put("error", e.getClass().getName());
        }

        return status;
    }

    @Override
    public Map<String, Object> getSystemMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // Get runtime info
        Runtime runtime = Runtime.getRuntime();

        // Memory metrics
        metrics.put("availableProcessors", runtime.availableProcessors());
        metrics.put("freeMemoryBytes", runtime.freeMemory());
        metrics.put("totalMemoryBytes", runtime.totalMemory());
        metrics.put("maxMemoryBytes", runtime.maxMemory());

        // Percentages for easier interpretation
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        double memoryUsagePercentage = ((double) usedMemory / runtime.maxMemory()) * 100;
        metrics.put("memoryUsagePercentage", String.format("%.2f", memoryUsagePercentage));

        // OS info
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        metrics.put("osName", osBean.getName());
        metrics.put("osVersion", osBean.getVersion());
        metrics.put("osArchitecture", osBean.getArch());
        metrics.put("systemLoadAverage", osBean.getSystemLoadAverage());

        // Memory details
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        metrics.put("heapMemoryUsage", memoryBean.getHeapMemoryUsage().toString());
        metrics.put("nonHeapMemoryUsage", memoryBean.getNonHeapMemoryUsage().toString());

        // JVM uptime
        long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
        metrics.put("uptimeMillis", uptimeMillis);
        metrics.put("uptimeFormatted", formatUptime(uptimeMillis));

        return metrics;
    }

    /**
     * Helper method to create a MultipartFile from a string for testing
     */
    private MultipartFile createMultipartFileFromString(String filename, String content) {
        return new MultipartFile() {
            @Override
            public String getName() {
                return "file";
            }

            @Override
            public String getOriginalFilename() {
                return filename;
            }

            @Override
            public String getContentType() {
                return "text/plain";
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public long getSize() {
                return content.getBytes().length;
            }

            @Override
            public byte[] getBytes() throws IOException {
                return content.getBytes();
            }

            @Override
            public java.io.InputStream getInputStream() throws IOException {
                return new ByteArrayInputStream(content.getBytes());
            }

            @Override
            public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
                java.nio.file.Files.write(dest.toPath(), content.getBytes());
            }
        };
    }

    /**
     * Format uptime into a human-readable string
     */
    private String formatUptime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        hours %= 24;
        minutes %= 60;
        seconds %= 60;

        StringBuilder uptime = new StringBuilder();
        if (days > 0) uptime.append(days).append("d ");
        if (hours > 0) uptime.append(hours).append("h ");
        if (minutes > 0) uptime.append(minutes).append("m ");
        uptime.append(seconds).append("s");

        return uptime.toString().trim();
    }
}