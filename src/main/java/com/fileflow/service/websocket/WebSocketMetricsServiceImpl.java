package com.fileflow.service.websocket;

import com.fileflow.model.WebSocketMetrics;
import com.fileflow.repository.WebSocketMetricsRepository;
import com.fileflow.repository.WebSocketSessionRepository;
import com.fileflow.repository.WebSocketSubscriptionRepository;
import com.fileflow.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of the WebSocketMetricsService interface
 * Provides methods for gathering and analyzing WebSocket usage metrics
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketMetricsServiceImpl implements WebSocketMetricsService {

    private final WebSocketMetricsRepository metricsRepository;
    private final WebSocketSessionRepository sessionRepository;
    private final WebSocketSubscriptionRepository subscriptionRepository;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getMetricsForDateRange(LocalDate startDate, LocalDate endDate) {
        log.debug("Getting WebSocket metrics for date range: {} to {}", startDate, endDate);

        try {
            // Get aggregate metrics
            Map<String, Object> aggregateMetrics = metricsRepository.getTotalMetricsForDateRange(
                    startDate, endDate);

            if (aggregateMetrics == null) {
                // If no data available, return empty stats
                Map<String, Object> emptyStats = new HashMap<>();
                emptyStats.put("totalConnections", 0);
                emptyStats.put("totalMessagesSent", 0);
                emptyStats.put("totalMessagesReceived", 0);
                emptyStats.put("totalErrors", 0);
                emptyStats.put("avgMessageSize", 0);

                return Map.of(
                        "startDate", startDate,
                        "endDate", endDate,
                        "aggregate", emptyStats,
                        "daily", Map.of(),
                        "noData", true
                );
            }

            // Get metrics by date
            List<WebSocketMetrics> dailyMetrics = metricsRepository.findByEventDateBetweenOrderByEventDateAscHourOfDayAsc(
                    startDate, endDate);

            // Process metrics by day
            Map<String, Map<String, Object>> metricsByDay = processMetricsByDay(dailyMetrics);

            // Process metrics by hour (average per hour of day)
            Map<Integer, Map<String, Object>> metricsByHour = processMetricsByHour(dailyMetrics);

            // Calculate trends
            Map<String, Object> trends = calculateTrends(startDate, endDate);

            // Combine all metrics
            Map<String, Object> result = new HashMap<>();
            result.put("startDate", startDate);
            result.put("endDate", endDate);
            result.put("aggregate", aggregateMetrics);
            result.put("daily", metricsByDay);
            result.put("hourly", metricsByHour);
            result.put("trends", trends);

            return result;
        } catch (Exception e) {
            log.error("Error retrieving WebSocket metrics: {}", e.getMessage(), e);
            return Map.of(
                    "error", "Failed to retrieve WebSocket metrics",
                    "startDate", startDate,
                    "endDate", endDate
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getWebSocketStats() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
                return Map.of("error", "User not authenticated");
            }

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            Long userId = userPrincipal.getId();

            // Get active sessions
            List<com.fileflow.model.WebSocketSession> activeSessions =
                    sessionRepository.findByUser_IdAndIsActiveTrue(userId);

            // Get active subscriptions
            List<com.fileflow.model.WebSocketSubscription> activeSubscriptions =
                    subscriptionRepository.findByUser_IdAndIsActiveTrue(userId);

            // Count by type
            long fileSubscriptions = activeSubscriptions.stream()
                    .filter(s -> s.getItemType() == com.fileflow.model.WebSocketSubscription.ItemType.FILE)
                    .count();

            long folderSubscriptions = activeSubscriptions.stream()
                    .filter(s -> s.getItemType() == com.fileflow.model.WebSocketSubscription.ItemType.FOLDER)
                    .count();

            // Get last activity time if any sessions exist
            LocalDateTime lastActivity = null;
            if (!activeSessions.isEmpty()) {
                lastActivity = activeSessions.stream()
                        .map(com.fileflow.model.WebSocketSession::getLastActivity)
                        .max(LocalDateTime::compareTo)
                        .orElse(null);
            }

            // Build response
            Map<String, Object> stats = new HashMap<>();
            stats.put("userId", userId);
            stats.put("connected", !activeSessions.isEmpty());
            stats.put("activeSessions", activeSessions.size());
            stats.put("lastActivity", lastActivity);
            stats.put("activeSubscriptions", activeSubscriptions.size());
            stats.put("fileSubscriptions", fileSubscriptions);
            stats.put("folderSubscriptions", folderSubscriptions);

            // Get subscription details (limited to avoid excessive data)
            if (activeSubscriptions.size() <= 20) {  // Only show details for a reasonable number
                List<Map<String, Object>> subscriptionDetails = activeSubscriptions.stream()
                        .map(this::mapSubscriptionToDetails)
                        .collect(Collectors.toList());
                stats.put("subscriptionDetails", subscriptionDetails);
            }

            // Add session details (limited to avoid excessive data)
            if (activeSessions.size() <= 5) {  // Only show details for a reasonable number
                List<Map<String, Object>> sessionDetails = activeSessions.stream()
                        .map(this::mapSessionToDetails)
                        .collect(Collectors.toList());
                stats.put("sessionDetails", sessionDetails);
            }

            return stats;
        } catch (Exception e) {
            log.error("Error getting WebSocket stats: {}", e.getMessage(), e);
            return Map.of("error", "Failed to retrieve WebSocket statistics");
        }
    }

    /**
     * Record WebSocket metrics
     */
    @Transactional
    public void recordMetrics(int messagesReceived, int messagesSent, int errors, int averageMessageSize) {
        try {
            LocalDate today = LocalDate.now();
            int hour = LocalTime.now().getHour();

            Optional<WebSocketMetrics> existingMetrics = metricsRepository.findByEventDateAndHourOfDay(today, hour);

            WebSocketMetrics metrics;
            if (existingMetrics.isPresent()) {
                metrics = existingMetrics.get();

                // Update metrics
                if (messagesReceived > 0) {
                    metrics.setMessagesReceived(metrics.getMessagesReceived() + messagesReceived);
                }

                if (messagesSent > 0) {
                    metrics.setMessagesSent(metrics.getMessagesSent() + messagesSent);
                }

                if (errors > 0) {
                    metrics.setErrorsCount(metrics.getErrorsCount() + errors);
                }

                if (averageMessageSize > 0) {
                    // Update average message size
                    if (metrics.getAverageMessageSize() == null) {
                        metrics.setAverageMessageSize(averageMessageSize);
                    } else {
                        int totalMessages = metrics.getMessagesSent() + metrics.getMessagesReceived();
                        int previousTotal = totalMessages - 1;
                        int newAverage = (metrics.getAverageMessageSize() * previousTotal + averageMessageSize) / totalMessages;
                        metrics.setAverageMessageSize(newAverage);
                    }
                }
            } else {
                // Create new metrics
                metrics = WebSocketMetrics.builder()
                        .eventDate(today)
                        .hourOfDay(hour)
                        .activeConnections(countActiveConnections())
                        .messagesReceived(messagesReceived)
                        .messagesSent(messagesSent)
                        .errorsCount(errors)
                        .averageMessageSize(averageMessageSize > 0 ? averageMessageSize : null)
                        .build();
            }

            metricsRepository.save(metrics);
        } catch (Exception e) {
            log.error("Error recording WebSocket metrics: {}", e.getMessage(), e);
        }
    }

    /**
     * Count active connections
     */
    public int countActiveConnections() {
        try {
            return (int) sessionRepository.count();
        } catch (Exception e) {
            log.error("Error counting active connections: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Process metrics by day
     */
    private Map<String, Map<String, Object>> processMetricsByDay(List<WebSocketMetrics> metrics) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;

        return metrics.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getEventDate().format(formatter),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    Map<String, Object> dayMetrics = new HashMap<>();
                                    dayMetrics.put("activeConnections", list.stream()
                                            .mapToInt(WebSocketMetrics::getActiveConnections).max().orElse(0));
                                    dayMetrics.put("messagesSent", list.stream()
                                            .mapToInt(WebSocketMetrics::getMessagesSent).sum());
                                    dayMetrics.put("messagesReceived", list.stream()
                                            .mapToInt(WebSocketMetrics::getMessagesReceived).sum());
                                    dayMetrics.put("errors", list.stream()
                                            .mapToInt(WebSocketMetrics::getErrorsCount).sum());

                                    // Calculate averages
                                    double avgMessageSize = list.stream()
                                            .filter(m -> m.getAverageMessageSize() != null)
                                            .mapToInt(WebSocketMetrics::getAverageMessageSize)
                                            .average()
                                            .orElse(0);
                                    dayMetrics.put("avgMessageSize", avgMessageSize);

                                    return dayMetrics;
                                }
                        )
                ));
    }

    /**
     * Process metrics by hour of day (averages)
     */
    private Map<Integer, Map<String, Object>> processMetricsByHour(List<WebSocketMetrics> metrics) {
        return metrics.stream()
                .collect(Collectors.groupingBy(
                        WebSocketMetrics::getHourOfDay,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    Map<String, Object> hourMetrics = new HashMap<>();
                                    hourMetrics.put("activeConnections", list.stream()
                                            .mapToInt(WebSocketMetrics::getActiveConnections)
                                            .average().orElse(0));
                                    hourMetrics.put("messagesSent", list.stream()
                                            .mapToInt(WebSocketMetrics::getMessagesSent)
                                            .average().orElse(0));
                                    hourMetrics.put("messagesReceived", list.stream()
                                            .mapToInt(WebSocketMetrics::getMessagesReceived)
                                            .average().orElse(0));
                                    hourMetrics.put("errors", list.stream()
                                            .mapToInt(WebSocketMetrics::getErrorsCount)
                                            .average().orElse(0));

                                    // Calculate average message size
                                    double avgMessageSize = list.stream()
                                            .filter(m -> m.getAverageMessageSize() != null)
                                            .mapToInt(WebSocketMetrics::getAverageMessageSize)
                                            .average()
                                            .orElse(0);
                                    hourMetrics.put("avgMessageSize", avgMessageSize);

                                    return hourMetrics;
                                }
                        )
                ));
    }

    /**
     * Calculate trends by comparing with previous period
     */
    private Map<String, Object> calculateTrends(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> trends = new HashMap<>();

        try {
            // Calculate duration of the period
            long days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;

            // Calculate previous period
            LocalDate previousStartDate = startDate.minusDays(days);
            LocalDate previousEndDate = startDate.minusDays(1);

            // Get metrics for current period
            Map<String, Object> currentMetrics = metricsRepository.getTotalMetricsForDateRange(
                    startDate, endDate);

            // Get metrics for previous period
            Map<String, Object> previousMetrics = metricsRepository.getTotalMetricsForDateRange(
                    previousStartDate, previousEndDate);

            // If no previous data, return current only
            if (previousMetrics == null) {
                trends.put("messagesSentGrowth", 0);
                trends.put("messagesReceivedGrowth", 0);
                trends.put("errorsGrowth", 0);
                trends.put("hasPreviousPeriodData", false);
                return trends;
            }

            // Calculate growth percentages
            long currentMessagesSent = Long.parseLong(currentMetrics.get("totalMessagesSent").toString());
            long previousMessagesSent = Long.parseLong(previousMetrics.get("totalMessagesSent").toString());

            long currentMessagesReceived = Long.parseLong(currentMetrics.get("totalMessagesReceived").toString());
            long previousMessagesReceived = Long.parseLong(previousMetrics.get("totalMessagesReceived").toString());

            long currentErrors = Long.parseLong(currentMetrics.get("totalErrors").toString());
            long previousErrors = Long.parseLong(previousMetrics.get("totalErrors").toString());

            // Calculate percentage changes
            double messagesSentGrowth = previousMessagesSent > 0 ?
                    ((double) currentMessagesSent - previousMessagesSent) * 100 / previousMessagesSent : 0;

            double messagesReceivedGrowth = previousMessagesReceived > 0 ?
                    ((double) currentMessagesReceived - previousMessagesReceived) * 100 / previousMessagesReceived : 0;

            double errorsGrowth = previousErrors > 0 ?
                    ((double) currentErrors - previousErrors) * 100 / previousErrors : 0;

            // Add to trends
            trends.put("messagesSentGrowth", messagesSentGrowth);
            trends.put("messagesReceivedGrowth", messagesReceivedGrowth);
            trends.put("errorsGrowth", errorsGrowth);
            trends.put("hasPreviousPeriodData", true);
            trends.put("previousPeriodStart", previousStartDate);
            trends.put("previousPeriodEnd", previousEndDate);
        } catch (Exception e) {
            log.error("Error calculating WebSocket metric trends: {}", e.getMessage(), e);
            trends.put("error", "Failed to calculate trends");
        }

        return trends;
    }

    /**
     * Map a WebSocketSubscription entity to a details map
     */
    private Map<String, Object> mapSubscriptionToDetails(com.fileflow.model.WebSocketSubscription subscription) {
        Map<String, Object> details = new HashMap<>();
        details.put("id", subscription.getId());
        details.put("itemId", subscription.getItemId());
        details.put("itemType", subscription.getItemType().name());
        details.put("createdAt", subscription.getCreatedAt());
        details.put("updatedAt", subscription.getUpdatedAt());
        return details;
    }

    /**
     * Map a WebSocketSession entity to a details map
     */
    private Map<String, Object> mapSessionToDetails(com.fileflow.model.WebSocketSession session) {
        Map<String, Object> details = new HashMap<>();
        details.put("id", session.getId());
        details.put("sessionId", session.getSessionId());
        details.put("connectedAt", session.getConnectedAt());
        details.put("lastActivity", session.getLastActivity());
        details.put("ipAddress", session.getIpAddress());

        // Trim user agent to avoid excessive data
        String userAgent = session.getUserAgent();
        if (userAgent != null && userAgent.length() > 50) {
            userAgent = userAgent.substring(0, 47) + "...";
        }
        details.put("userAgent", userAgent);

        return details;
    }
}