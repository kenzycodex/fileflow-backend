package com.fileflow.service.health;

import com.fileflow.dto.response.health.HealthCheckResponse;
import com.fileflow.repository.UserRepository;
import com.fileflow.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Status;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import java.io.InputStream;
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
    private final StorageService storageService;
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
            // Create a test file
            String testFileName = "health-check-" + UUID.randomUUID().toString() + ".txt";
            String testContent = "Health check test file - " + LocalDateTime.now();

            // Convert string to input stream
            InputStream inputStream = new java.io.ByteArrayInputStream(testContent.getBytes());

            // Store test file
            String storagePath = storageService.store(inputStream, testContent.getBytes().length, testFileName, "health-checks");

            // Check if file exists
            boolean exists = storageService.exists(storagePath);

            if (exists) {
                status.put("status", "UP");
                status.put("message", "Storage service is operational");

                // Clean up test file
                storageService.delete(storagePath);
            } else {
                status.put("message", "Test file not found after upload");
            }
        } catch (Exception e) {
            log.error("Storage health check failed", e);
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

        metrics.put("availableProcessors", runtime.availableProcessors());
        metrics.put("freeMemory", runtime.freeMemory());
        metrics.put("totalMemory", runtime.totalMemory());
        metrics.put("maxMemory", runtime.maxMemory());

        // Get OS info
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        metrics.put("osName", osBean.getName());
        metrics.put("osVersion", osBean.getVersion());
        metrics.put("osArch", osBean.getArch());
        metrics.put("systemLoadAverage", osBean.getSystemLoadAverage());

        // Get memory details
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        metrics.put("heapMemoryUsage", memoryBean.getHeapMemoryUsage().toString());
        metrics.put("nonHeapMemoryUsage", memoryBean.getNonHeapMemoryUsage().toString());

        // Get JVM uptime
        metrics.put("uptime", ManagementFactory.getRuntimeMXBean().getUptime());

        return metrics;
    }
}