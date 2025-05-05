package com.fileflow.service.statistics;

import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.exception.ForbiddenException;
import com.fileflow.model.*;
import com.fileflow.repository.*;
import com.fileflow.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Enhanced implementation of the StatisticsService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsServiceImpl implements StatisticsService {

    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final ShareRepository shareRepository;
    private final ActivityRepository activityRepository;
    private final WebSocketMetricsRepository webSocketMetricsRepository;
    private final WebSocketSessionRepository webSocketSessionRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "storageStatistics", key = "#root.methodName + '_' + @authenticationFacade.getUserId()")
    public ApiResponse getStorageStatistics() {
        User currentUser = getCurrentUser();

        // Get files (not deleted)
        List<File> files = fileRepository.findByUserAndIsDeletedFalse(currentUser);

        // Calculate storage metrics
        long totalFiles = files.size();
        long totalSize = files.stream().mapToLong(File::getFileSize).sum();
        long totalFolders = folderRepository.countByUserAndIsDeletedFalse(currentUser);
        double quotaUsedPercentage = currentUser.getStorageQuota() > 0 ?
                (double) totalSize * 100 / currentUser.getStorageQuota() : 0;
        long availableSpace = currentUser.getStorageQuota() - totalSize;

        // Format quota percentage with 2 decimal places
        BigDecimal quotaPercentage = BigDecimal.valueOf(quotaUsedPercentage)
                .setScale(2, RoundingMode.HALF_UP);

        // Count by file type
        Map<String, Long> fileTypeCount = files.stream()
                .collect(Collectors.groupingBy(
                        file -> file.getFileType() != null ? file.getFileType() : "unknown",
                        Collectors.counting()
                ));

        // Calculate file type size distribution
        Map<String, Long> fileTypeSize = files.stream()
                .collect(Collectors.groupingBy(
                        file -> file.getFileType() != null ? file.getFileType() : "unknown",
                        Collectors.summingLong(File::getFileSize)
                ));

        // Calculate largest files
        List<Map<String, Object>> largestFiles = files.stream()
                .sorted(Comparator.comparingLong(File::getFileSize).reversed())
                .limit(5)
                .map(file -> {
                    Map<String, Object> fileInfo = new HashMap<>();
                    fileInfo.put("id", file.getId());
                    fileInfo.put("filename", file.getFilename());
                    fileInfo.put("fileSize", file.getFileSize());
                    fileInfo.put("fileType", file.getFileType());
                    fileInfo.put("createdAt", file.getCreatedAt());
                    return fileInfo;
                })
                .collect(Collectors.toList());

        // Calculate recently uploaded files
        List<Map<String, Object>> recentFiles = files.stream()
                .sorted(Comparator.comparing(File::getCreatedAt).reversed())
                .limit(5)
                .map(file -> {
                    Map<String, Object> fileInfo = new HashMap<>();
                    fileInfo.put("id", file.getId());
                    fileInfo.put("filename", file.getFilename());
                    fileInfo.put("fileSize", file.getFileSize());
                    fileInfo.put("fileType", file.getFileType());
                    fileInfo.put("createdAt", file.getCreatedAt());
                    return fileInfo;
                })
                .collect(Collectors.toList());

        // Calculate growth metrics (compare with 30 days ago)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long filesThirtyDaysAgo = fileRepository.countByUserAndCreatedAtBeforeAndIsDeletedFalse(
                currentUser, thirtyDaysAgo);
        long sizeThirtyDaysAgo = fileRepository.sumFileSizeByUserAndCreatedAtBeforeAndIsDeletedFalse(
                currentUser, thirtyDaysAgo);

        // Calculate growth percentages
        double fileGrowthPercentage = filesThirtyDaysAgo > 0 ?
                (totalFiles - filesThirtyDaysAgo) * 100.0 / filesThirtyDaysAgo : 0;
        double sizeGrowthPercentage = sizeThirtyDaysAgo > 0 ?
                (totalSize - sizeThirtyDaysAgo) * 100.0 / sizeThirtyDaysAgo : 0;

        // Format growth percentages with 2 decimal places
        BigDecimal fileGrowth = BigDecimal.valueOf(fileGrowthPercentage)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal sizeGrowth = BigDecimal.valueOf(sizeGrowthPercentage)
                .setScale(2, RoundingMode.HALF_UP);

        // Build response data
        Map<String, Object> data = new HashMap<>();
        data.put("totalFiles", totalFiles);
        data.put("totalFolders", totalFolders);
        data.put("totalSize", totalSize);
        data.put("availableSpace", availableSpace);
        data.put("quota", currentUser.getStorageQuota());
        data.put("quotaUsedPercentage", quotaPercentage);
        data.put("fileTypeCount", fileTypeCount);
        data.put("fileTypeSize", fileTypeSize);
        data.put("largestFiles", largestFiles);
        data.put("recentFiles", recentFiles);
        data.put("fileGrowth", fileGrowth);
        data.put("sizeGrowth", sizeGrowth);
        data.put("lastUpdated", LocalDateTime.now());

        return ApiResponse.builder()
                .success(true)
                .message("Storage statistics retrieved successfully")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "activityStatistics", key = "#root.methodName + '_' + @authenticationFacade.getUserId() + '_' + #startDate + '_' + #endDate")
    public ApiResponse getActivityStatistics(LocalDate startDate, LocalDate endDate) {
        User currentUser = getCurrentUser();

        // Set default date range if not provided
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }

        if (endDate == null) {
            endDate = LocalDate.now();
        }

        // Convert to LocalDateTime for query
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // Get activities within date range
        List<Activity> activities = activityRepository.findByUserAndCreatedAtBetween(
                currentUser, startDateTime, endDateTime);

        // Count by activity type
        Map<String, Long> activityTypeCount = activities.stream()
                .collect(Collectors.groupingBy(Activity::getActivityType, Collectors.counting()));

        // Group by date - ensure all dates in range are represented
        Map<String, Long> activityByDate = getActivityCountByDate(activities, startDate, endDate);

        // Get most active hours
        Map<Integer, Long> activityByHour = activities.stream()
                .collect(Collectors.groupingBy(
                        activity -> activity.getCreatedAt().getHour(),
                        Collectors.counting()));

        // Get most active days of week
        Map<String, Long> activityByDayOfWeek = activities.stream()
                .collect(Collectors.groupingBy(
                        activity -> activity.getCreatedAt().getDayOfWeek().toString(),
                        Collectors.counting()));

        // Get item types that are being acted on
        Map<String, Long> activityByItemType = activities.stream()
                .collect(Collectors.groupingBy(Activity::getItemType, Collectors.counting()));

        // Recent activities - limit to last 10
        List<Map<String, Object>> recentActivities = activities.stream()
                .sorted(Comparator.comparing(Activity::getCreatedAt).reversed())
                .limit(10)
                .map(activity -> {
                    Map<String, Object> activityInfo = new HashMap<>();
                    activityInfo.put("id", activity.getId());
                    activityInfo.put("activityType", activity.getActivityType());
                    activityInfo.put("itemType", activity.getItemType());
                    activityInfo.put("itemId", activity.getItemId());
                    activityInfo.put("description", activity.getDescription());
                    activityInfo.put("createdAt", activity.getCreatedAt());
                    return activityInfo;
                })
                .collect(Collectors.toList());

        // Get user session statistics if available
        Map<String, Object> sessionStats = getUserSessionStats(currentUser.getId());

        // Prepare activity trends (compare with previous period)
        Map<String, Object> activityTrends = calculateActivityTrends(
                currentUser, startDateTime, endDateTime);

        // Build response data
        Map<String, Object> data = new HashMap<>();
        data.put("totalActivities", activities.size());
        data.put("activityTypeCount", activityTypeCount);
        data.put("activityByDate", activityByDate);
        data.put("activityByHour", activityByHour);
        data.put("activityByDayOfWeek", activityByDayOfWeek);
        data.put("activityByItemType", activityByItemType);
        data.put("recentActivities", recentActivities);
        data.put("sessionStats", sessionStats);
        data.put("trends", activityTrends);
        data.put("startDate", startDate);
        data.put("endDate", endDate);
        data.put("lastUpdated", LocalDateTime.now());

        return ApiResponse.builder()
                .success(true)
                .message("Activity statistics retrieved successfully")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "fileTypeStatistics", key = "#root.methodName + '_' + @authenticationFacade.getUserId()")
    public ApiResponse getFileTypeStatistics() {
        User currentUser = getCurrentUser();

        // Get files (not deleted)
        List<File> files = fileRepository.findByUserAndIsDeletedFalse(currentUser);

        // Group by file type
        Map<String, Long> fileTypeCount = files.stream()
                .collect(Collectors.groupingBy(
                        file -> file.getFileType() != null ? file.getFileType() : "unknown",
                        Collectors.counting()));

        // Group by file size
        Map<String, Long> fileTypeSize = files.stream()
                .collect(Collectors.groupingBy(
                        file -> file.getFileType() != null ? file.getFileType() : "unknown",
                        Collectors.summingLong(File::getFileSize)));

        // Average file size by type
        Map<String, Double> avgFileSizeByType = new HashMap<>();
        fileTypeCount.forEach((type, count) -> {
            if (count > 0) {
                avgFileSizeByType.put(type, fileTypeSize.get(type) / (double) count);
            }
        });

        // File counts by size categories
        Map<String, Long> fileSizeCategories = new HashMap<>();
        fileSizeCategories.put("Tiny (< 10KB)",
                files.stream().filter(f -> f.getFileSize() < 10 * 1024).count());
        fileSizeCategories.put("Small (10KB - 100KB)",
                files.stream().filter(f -> f.getFileSize() >= 10 * 1024 && f.getFileSize() < 100 * 1024).count());
        fileSizeCategories.put("Medium (100KB - 1MB)",
                files.stream().filter(f -> f.getFileSize() >= 100 * 1024 && f.getFileSize() < 1024 * 1024).count());
        fileSizeCategories.put("Large (1MB - 10MB)",
                files.stream().filter(f -> f.getFileSize() >= 1024 * 1024 && f.getFileSize() < 10 * 1024 * 1024).count());
        fileSizeCategories.put("Very Large (10MB - 100MB)",
                files.stream().filter(f -> f.getFileSize() >= 10 * 1024 * 1024 && f.getFileSize() < 100 * 1024 * 1024).count());
        fileSizeCategories.put("Huge (> 100MB)",
                files.stream().filter(f -> f.getFileSize() >= 100 * 1024 * 1024).count());

        // File uploads by month (last 12 months)
        Map<String, Long> fileUploadsByMonth = getFileUploadsByMonth(currentUser);

        // MIME type distribution
        Map<String, Long> mimeTypeDistribution = files.stream()
                .filter(f -> f.getMimeType() != null)
                .collect(Collectors.groupingBy(File::getMimeType, Collectors.counting()));

        // Build response data
        Map<String, Object> data = new HashMap<>();
        data.put("fileTypeCount", fileTypeCount);
        data.put("fileTypeSize", fileTypeSize);
        data.put("avgFileSizeByType", avgFileSizeByType);
        data.put("fileSizeCategories", fileSizeCategories);
        data.put("fileUploadsByMonth", fileUploadsByMonth);
        data.put("mimeTypeDistribution", mimeTypeDistribution);
        data.put("totalFiles", files.size());
        data.put("lastUpdated", LocalDateTime.now());

        return ApiResponse.builder()
                .success(true)
                .message("File type statistics retrieved successfully")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "sharingStatistics", key = "#root.methodName + '_' + @authenticationFacade.getUserId()")
    public ApiResponse getSharingStatistics() {
        User currentUser = getCurrentUser();

        // Get outgoing shares
        List<Share> outgoingShares = shareRepository.findByOwner(currentUser);

        // Get incoming shares
        List<Share> incomingShares = shareRepository.findByRecipient(currentUser);

        // Enhance shares with item names
        outgoingShares.forEach(share -> {
            if (share.getItemType() == Share.ItemType.FILE) {
                fileRepository.findById(share.getItemId())
                        .ifPresent(file -> share.setItemName(file.getFilename()));
            } else if (share.getItemType() == Share.ItemType.FOLDER) {
                folderRepository.findById(share.getItemId())
                        .ifPresent(folder -> share.setItemName(folder.getFolderName()));
            }
        });

        // Count by permission type (outgoing)
        Map<Share.Permission, Long> permissionCount = outgoingShares.stream()
                .collect(Collectors.groupingBy(Share::getPermissions, Collectors.counting()));

        // Count by item type (outgoing)
        Map<Share.ItemType, Long> itemTypeCount = outgoingShares.stream()
                .collect(Collectors.groupingBy(Share::getItemType, Collectors.counting()));

        // Count password-protected shares
        long passwordProtectedCount = outgoingShares.stream()
                .filter(Share::isPasswordProtected)
                .count();

        // Count shares with expiry date
        long expiryDateCount = outgoingShares.stream()
                .filter(share -> share.getExpiryDate() != null)
                .count();

        // Count shares by recipient
        Map<String, Long> sharesByRecipient = outgoingShares.stream()
                .collect(Collectors.groupingBy(
                        share -> share.getRecipient().getUsername(),
                        Collectors.counting()));

        // Get sharing history by month
        Map<String, Long> sharingHistoryByMonth = getSharingHistoryByMonth(currentUser);

        // Calculate average view count for shares
        double avgViewCount = outgoingShares.stream()
                .mapToInt(Share::getViewCount)
                .average()
                .orElse(0);

        // Get most viewed shares
        List<Map<String, Object>> mostViewedShares = outgoingShares.stream()
                .sorted(Comparator.comparingInt(Share::getViewCount).reversed())
                .limit(5)
                .map(share -> {
                    Map<String, Object> shareInfo = new HashMap<>();
                    shareInfo.put("id", share.getId());
                    shareInfo.put("itemType", share.getItemType());
                    shareInfo.put("itemId", share.getItemId());
                    shareInfo.put("itemName", share.getItemName());
                    shareInfo.put("permissions", share.getPermissions());
                    shareInfo.put("recipient", share.getRecipient().getUsername());
                    shareInfo.put("viewCount", share.getViewCount());
                    shareInfo.put("createdAt", share.getCreatedAt());
                    return shareInfo;
                })
                .collect(Collectors.toList());

        // Calculate expiring shares (next 7 days)
        List<Map<String, Object>> expiringShares = outgoingShares.stream()
                .filter(share -> share.getExpiryDate() != null &&
                        share.getExpiryDate().isBefore(LocalDateTime.now().plusDays(7)) &&
                        share.getExpiryDate().isAfter(LocalDateTime.now()))
                .map(share -> {
                    Map<String, Object> shareInfo = new HashMap<>();
                    shareInfo.put("id", share.getId());
                    shareInfo.put("itemType", share.getItemType());
                    shareInfo.put("itemId", share.getItemId());
                    shareInfo.put("itemName", share.getItemName());
                    shareInfo.put("permissions", share.getPermissions());
                    shareInfo.put("recipient", share.getRecipient().getUsername());
                    shareInfo.put("expiryDate", share.getExpiryDate());
                    shareInfo.put("daysLeft", ChronoUnit.DAYS.between(LocalDateTime.now(), share.getExpiryDate()));
                    return shareInfo;
                })
                .collect(Collectors.toList());

        // Build response data
        Map<String, Object> data = new HashMap<>();
        data.put("totalOutgoingShares", outgoingShares.size());
        data.put("totalIncomingShares", incomingShares.size());
        data.put("permissionCount", permissionCount);
        data.put("itemTypeCount", itemTypeCount);
        data.put("passwordProtectedCount", passwordProtectedCount);
        data.put("expiryDateCount", expiryDateCount);
        data.put("sharesByRecipient", sharesByRecipient);
        data.put("sharingHistoryByMonth", sharingHistoryByMonth);
        data.put("avgViewCount", avgViewCount);
        data.put("mostViewedShares", mostViewedShares);
        data.put("expiringShares", expiringShares);
        data.put("lastUpdated", LocalDateTime.now());

        return ApiResponse.builder()
                .success(true)
                .message("Sharing statistics retrieved successfully")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse getSystemStatistics() {
        // Check if user is admin
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        if (!userPrincipal.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            throw new ForbiddenException("Only admins can access system statistics");
        }

        // Get all users
        long totalUsers = userRepository.count();

        // Get active users (who logged in within last 30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long activeUsers = userRepository.countByLastLoginAfter(thirtyDaysAgo);

        // Get total files
        long totalFiles = fileRepository.count();

        // Get total deleted files
        long totalDeletedFiles = fileRepository.countByIsDeletedTrue();

        // Get total folders
        long totalFolders = folderRepository.count();

        // Get total storage used
        long totalStorageUsed = userRepository.findAll().stream()
                .mapToLong(User::getStorageUsed)
                .sum();

        // Get formatted storage used (GB)
        double totalStorageUsedGB = totalStorageUsed / (1024.0 * 1024 * 1024);
        BigDecimal formattedStorageUsedGB = BigDecimal.valueOf(totalStorageUsedGB)
                .setScale(2, RoundingMode.HALF_UP);

        // Get total shares
        long totalShares = shareRepository.count();

        // Get user statistics by role
        Map<String, Long> usersByRole = getUserCountByRole();

        // Get storage statistics by file type
        Map<String, Long> storageByFileType = getStorageByFileType();

        // Get recently registered users
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<User> recentUsers = userRepository.findAll(pageable);

        // Get users by storage used (top 10)
        List<Map<String, Object>> topUsersByStorage = userRepository.findAll().stream()
                .sorted(Comparator.comparingLong(User::getStorageUsed).reversed())
                .limit(10)
                .map(user -> {
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("id", user.getId());
                    userInfo.put("username", user.getUsername());
                    userInfo.put("email", user.getEmail());
                    userInfo.put("fullName", user.getFullName());
                    userInfo.put("storageUsed", user.getStorageUsed());
                    userInfo.put("storageQuota", user.getStorageQuota());
                    userInfo.put("usagePercentage",
                            user.getStorageQuota() > 0 ?
                                    (double) user.getStorageUsed() * 100 / user.getStorageQuota() : 0);
                    return userInfo;
                })
                .collect(Collectors.toList());

        // Get system activity by date (last 30 days)
        Map<String, Long> systemActivityByDate = getSystemActivityByDate(thirtyDaysAgo, LocalDateTime.now());

        // Get file uploads by date (last 30 days)
        Map<String, Long> fileUploadsByDate = getFileUploadsByDate(thirtyDaysAgo, LocalDateTime.now());

        // Get WebSocket metrics for last 30 days
        Map<String, Object> webSocketMetrics = getWebSocketMetricsForLastDays(30);

        // Build response data
        Map<String, Object> data = new HashMap<>();
        data.put("totalUsers", totalUsers);
        data.put("activeUsers", activeUsers);
        data.put("totalFiles", totalFiles);
        data.put("totalDeletedFiles", totalDeletedFiles);
        data.put("totalFolders", totalFolders);
        data.put("totalStorageUsed", totalStorageUsed);
        data.put("totalStorageUsedGB", formattedStorageUsedGB);
        data.put("totalShares", totalShares);
        data.put("usersByRole", usersByRole);
        data.put("storageByFileType", storageByFileType);
        data.put("recentUsers", recentUsers.getContent());
        data.put("topUsersByStorage", topUsersByStorage);
        data.put("systemActivityByDate", systemActivityByDate);
        data.put("fileUploadsByDate", fileUploadsByDate);
        data.put("webSocketMetrics", webSocketMetrics);
        data.put("lastUpdated", LocalDateTime.now());

        return ApiResponse.builder()
                .success(true)
                .message("System statistics retrieved successfully")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // Helper methods

    /**
     * Get current authenticated user
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * Get activity count by date, ensuring all dates in range are represented
     */
    private Map<String, Long> getActivityCountByDate(List<Activity> activities, LocalDate startDate, LocalDate endDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Initialize map with all dates in range
        Map<String, Long> activityByDate = IntStream.rangeClosed(0, (int) ChronoUnit.DAYS.between(startDate, endDate))
                .mapToObj(startDate::plusDays)
                .collect(Collectors.toMap(
                        date -> date.format(formatter),
                        date -> 0L,
                        (a, b) -> a,
                        LinkedHashMap::new // Preserve insertion order
                ));

        // Fill in actual counts
        activityByDate.putAll(activities.stream()
                .collect(Collectors.groupingBy(
                        activity -> activity.getCreatedAt().toLocalDate().format(formatter),
                        Collectors.counting()
                )));

        return activityByDate;
    }

    /**
     * Get user session statistics
     */
    private Map<String, Object> getUserSessionStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();

        // Get active sessions
        List<com.fileflow.model.WebSocketSession> activeSessions =
                webSocketSessionRepository.findByUser_IdAndIsActiveTrue(userId);
        stats.put("activeSessionCount", activeSessions.size());

        // Get last activity time
        if (!activeSessions.isEmpty()) {
            LocalDateTime lastActivity = activeSessions.stream()
                    .map(com.fileflow.model.WebSocketSession::getLastActivity)
                    .max(Comparator.naturalOrder())
                    .orElse(null);
            stats.put("lastActivity", lastActivity);
        }

        return stats;
    }

    /**
     * Calculate activity trends compared to previous period
     */
    private Map<String, Object> calculateActivityTrends(User user, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        Map<String, Object> trends = new HashMap<>();

        // Calculate duration of the period
        long periodDays = ChronoUnit.DAYS.between(startDateTime.toLocalDate(), endDateTime.toLocalDate()) + 1;

        // Get activity count for current period
        long currentPeriodCount = activityRepository.countByUserAndCreatedAtBetween(
                user, startDateTime, endDateTime);

        // Calculate previous period date range
        LocalDateTime previousStartDateTime = startDateTime.minusDays(periodDays);
        LocalDateTime previousEndDateTime = endDateTime.minusDays(periodDays);

        // Get activity count for previous period
        long previousPeriodCount = activityRepository.countByUserAndCreatedAtBetween(
                user, previousStartDateTime, previousEndDateTime);

        // Calculate percentage change
        double percentageChange = previousPeriodCount > 0 ?
                (currentPeriodCount - previousPeriodCount) * 100.0 / previousPeriodCount : 0;

        // Format with 2 decimal places
        BigDecimal formattedChange = BigDecimal.valueOf(percentageChange)
                .setScale(2, RoundingMode.HALF_UP);

        trends.put("currentPeriodCount", currentPeriodCount);
        trends.put("previousPeriodCount", previousPeriodCount);
        trends.put("percentageChange", formattedChange);
        trends.put("improved", currentPeriodCount >= previousPeriodCount);

        return trends;
    }

    /**
     * Get file uploads by month for the last 12 months
     */
    private Map<String, Long> getFileUploadsByMonth(User user) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        // Get the last 12 months
        Map<String, Long> uploadsByMonth = IntStream.rangeClosed(0, 11)
                .mapToObj(i -> LocalDate.now().minusMonths(i))
                .collect(Collectors.toMap(
                        date -> date.format(formatter),
                        date -> 0L,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        // Get all files created in the last year
        LocalDateTime yearAgo = LocalDateTime.now().minusYears(1);
        List<File> recentFiles = fileRepository.findByUserAndCreatedAtAfter(user, yearAgo);

        // Group by month
        uploadsByMonth.putAll(recentFiles.stream()
                .collect(Collectors.groupingBy(
                        file -> file.getCreatedAt().format(formatter),
                        Collectors.counting()
                )));

        return uploadsByMonth;
    }

    /**
     * Get sharing history by month for the last 12 months
     */
    private Map<String, Long> getSharingHistoryByMonth(User user) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        // Get the last 12 months
        Map<String, Long> sharesByMonth = IntStream.rangeClosed(0, 11)
                .mapToObj(i -> LocalDate.now().minusMonths(i))
                .collect(Collectors.toMap(
                        date -> date.format(formatter),
                        date -> 0L,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        // Get all shares created in the last year
        LocalDateTime yearAgo = LocalDateTime.now().minusYears(1);
        List<Share> recentShares = shareRepository.findByOwnerAndCreatedAtAfter(user, yearAgo);

        // Group by month
        sharesByMonth.putAll(recentShares.stream()
                .collect(Collectors.groupingBy(
                        share -> share.getCreatedAt().format(formatter),
                        Collectors.counting()
                )));

        return sharesByMonth;
    }

    /**
     * Get user count by role
     */
    private Map<String, Long> getUserCountByRole() {
        List<User> allUsers = userRepository.findAll();

        Map<String, Long> usersByRole = new HashMap<>();
        allUsers.forEach(user -> {
            user.getRoles().forEach(role -> {
                String roleName = role.getName();
                usersByRole.put(roleName, usersByRole.getOrDefault(roleName, 0L) + 1);
            });
        });

        return usersByRole;
    }

    /**
     * Get storage used by file type
     */
    private Map<String, Long> getStorageByFileType() {
        List<File> allFiles = fileRepository.findAll();

        return allFiles.stream()
                .filter(file -> !file.isDeleted())
                .collect(Collectors.groupingBy(
                        file -> file.getFileType() != null ? file.getFileType() : "unknown",
                        Collectors.summingLong(File::getFileSize)
                ));
    }

    /**
     * Get system activity by date
     */
    private Map<String, Long> getSystemActivityByDate(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Initialize map with all dates in range
        Map<String, Long> activityByDate = IntStream.rangeClosed(0,
                        (int) ChronoUnit.DAYS.between(startDateTime.toLocalDate(), endDateTime.toLocalDate()))
                .mapToObj(startDateTime.toLocalDate()::plusDays)
                .collect(Collectors.toMap(
                        date -> date.format(formatter),
                        date -> 0L,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        // Get all activities in date range
        List<Activity> activities = activityRepository.findByCreatedAtBetween(startDateTime, endDateTime);

        // Group by date
        activityByDate.putAll(activities.stream()
                .collect(Collectors.groupingBy(
                        activity -> activity.getCreatedAt().toLocalDate().format(formatter),
                        Collectors.counting()
                )));

        return activityByDate;
    }

    /**
     * Get file uploads by date
     */
    private Map<String, Long> getFileUploadsByDate(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Initialize map with all dates in range
        Map<String, Long> uploadsByDate = IntStream.rangeClosed(0,
                        (int) ChronoUnit.DAYS.between(startDateTime.toLocalDate(), endDateTime.toLocalDate()))
                .mapToObj(startDateTime.toLocalDate()::plusDays)
                .collect(Collectors.toMap(
                        date -> date.format(formatter),
                        date -> 0L,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        // Get all files created in date range
        List<File> files = fileRepository.findByCreatedAtBetween(startDateTime, endDateTime);

        // Group by date
        uploadsByDate.putAll(files.stream()
                .collect(Collectors.groupingBy(
                        file -> file.getCreatedAt().toLocalDate().format(formatter),
                        Collectors.counting()
                )));

        return uploadsByDate;
    }

    /**
     * Get WebSocket metrics for the last N days
     */
    private Map<String, Object> getWebSocketMetricsForLastDays(int days) {
        LocalDate startDate = LocalDate.now().minusDays(days);
        LocalDate endDate = LocalDate.now();

        // Get aggregate metrics
        Map<String, Object> aggregateMetrics = webSocketMetricsRepository.getTotalMetricsForDateRange(
                startDate, endDate);

        // Get metrics by date
        List<WebSocketMetrics> dailyMetrics = webSocketMetricsRepository.findByEventDateBetweenOrderByEventDateAscHourOfDayAsc(
                startDate, endDate);

        // Aggregate by day
        Map<String, Map<String, Object>> metricsByDay = dailyMetrics.stream()
                .collect(Collectors.groupingBy(
                        metrics -> metrics.getEventDate().toString(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    Map<String, Object> dayMetrics = new HashMap<>();
                                    dayMetrics.put("totalConnections", list.stream()
                                            .mapToInt(WebSocketMetrics::getActiveConnections).sum());
                                    dayMetrics.put("totalMessagesSent", list.stream()
                                            .mapToInt(WebSocketMetrics::getMessagesSent).sum());
                                    dayMetrics.put("totalMessagesReceived", list.stream()
                                            .mapToInt(WebSocketMetrics::getMessagesReceived).sum());
                                    dayMetrics.put("totalErrors", list.stream()
                                            .mapToInt(WebSocketMetrics::getErrorsCount).sum());
                                    return dayMetrics;
                                }
                        )
                ));

        Map<String, Object> result = new HashMap<>();
        result.put("aggregate", aggregateMetrics);
        result.put("daily", metricsByDay);

        return result;
    }
}