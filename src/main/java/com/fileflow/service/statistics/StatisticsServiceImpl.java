package com.fileflow.service.statistics;

import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.exception.ForbiddenException;
import com.fileflow.model.Activity;
import com.fileflow.model.File;
import com.fileflow.model.Share;
import com.fileflow.model.User;
import com.fileflow.repository.*;
import com.fileflow.security.UserPrincipal;
import com.fileflow.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsServiceImpl implements StatisticsService {

    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final ShareRepository shareRepository;
    private final ActivityRepository activityRepository;

    @Override
    public ApiResponse getStorageStatistics() {
        User currentUser = getCurrentUser();

        // Get files (not deleted)
        List<File> files = fileRepository.findByUserAndIsDeletedFalse(currentUser);

        // Calculate statistics
        long totalFiles = files.size();
        long totalSize = files.stream().mapToLong(File::getFileSize).sum();
        long quotaUsedPercentage = currentUser.getStorageQuota() > 0 ?
                (totalSize * 100) / currentUser.getStorageQuota() : 0;
        long availableSpace = currentUser.getStorageQuota() - totalSize;

        // Count by file type
        Map<String, Long> fileTypeCount = files.stream()
                .collect(Collectors.groupingBy(File::getFileType, Collectors.counting()));

        // Get file type distribution
        Map<String, Long> fileTypeSize = files.stream()
                .collect(Collectors.groupingBy(File::getFileType,
                        Collectors.summingLong(File::getFileSize)));

        Map<String, Object> data = new HashMap<>();
        data.put("totalFiles", totalFiles);
        data.put("totalSize", totalSize);
        data.put("availableSpace", availableSpace);
        data.put("quota", currentUser.getStorageQuota());
        data.put("quotaUsedPercentage", quotaUsedPercentage);
        data.put("fileTypeCount", fileTypeCount);
        data.put("fileTypeSize", fileTypeSize);

        return ApiResponse.builder()
                .success(true)
                .message("Storage statistics retrieved successfully")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
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

        // Group by date
        Map<LocalDate, Long> activityByDate = activities.stream()
                .collect(Collectors.groupingBy(
                        activity -> activity.getCreatedAt().toLocalDate(),
                        Collectors.counting()));

        // Get most active hours
        Map<Integer, Long> activityByHour = activities.stream()
                .collect(Collectors.groupingBy(
                        activity -> activity.getCreatedAt().getHour(),
                        Collectors.counting()));

        Map<String, Object> data = new HashMap<>();
        data.put("totalActivities", activities.size());
        data.put("activityTypeCount", activityTypeCount);
        data.put("activityByDate", activityByDate);
        data.put("activityByHour", activityByHour);
        data.put("startDate", startDate);
        data.put("endDate", endDate);

        return ApiResponse.builder()
                .success(true)
                .message("Activity statistics retrieved successfully")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public ApiResponse getFileTypeStatistics() {
        User currentUser = getCurrentUser();

        // Get files (not deleted)
        List<File> files = fileRepository.findByUserAndIsDeletedFalse(currentUser);

        // Group by file type
        Map<String, Long> fileTypeCount = files.stream()
                .collect(Collectors.groupingBy(File::getFileType, Collectors.counting()));

        // Group by file size
        Map<String, Long> fileTypeSize = files.stream()
                .collect(Collectors.groupingBy(File::getFileType,
                        Collectors.summingLong(File::getFileSize)));

        // Average file size by type
        Map<String, Double> avgFileSizeByType = new HashMap<>();
        fileTypeCount.forEach((type, count) -> {
            if (count > 0) {
                avgFileSizeByType.put(type, fileTypeSize.get(type) / (double) count);
            }
        });

        Map<String, Object> data = new HashMap<>();
        data.put("fileTypeCount", fileTypeCount);
        data.put("fileTypeSize", fileTypeSize);
        data.put("avgFileSizeByType", avgFileSizeByType);

        return ApiResponse.builder()
                .success(true)
                .message("File type statistics retrieved successfully")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public ApiResponse getSharingStatistics() {
        User currentUser = getCurrentUser();

        // Get outgoing shares
        List<Share> outgoingShares = shareRepository.findByOwner(currentUser);

        // Get incoming shares
        List<Share> incomingShares = shareRepository.findByRecipient(currentUser);

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

        // Get most viewed shares
        List<Share> mostViewedShares = outgoingShares.stream()
                .sorted((s1, s2) -> Integer.compare(s2.getViewCount(), s1.getViewCount()))
                .limit(5)
                .collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("totalOutgoingShares", outgoingShares.size());
        data.put("totalIncomingShares", incomingShares.size());
        data.put("permissionCount", permissionCount);
        data.put("itemTypeCount", itemTypeCount);
        data.put("passwordProtectedCount", passwordProtectedCount);
        data.put("expiryDateCount", expiryDateCount);
        data.put("mostViewedShares", mostViewedShares);

        return ApiResponse.builder()
                .success(true)
                .message("Sharing statistics retrieved successfully")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
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

        // Get total folders
        long totalFolders = folderRepository.count();

        // Get total storage used
        long totalStorageUsed = userRepository.findAll().stream()
                .mapToLong(User::getStorageUsed)
                .sum();

        // Get total shares
        long totalShares = shareRepository.count();

        // Get recently registered users
        Pageable pageable = PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("createdAt").descending());
        Page<User> recentUsers = userRepository.findAll(pageable);

        // Get users by storage used (top 10)
        List<User> topUsersByStorage = userRepository.findAll().stream()
                .sorted((u1, u2) -> Long.compare(u2.getStorageUsed(), u1.getStorageUsed()))
                .limit(10)
                .collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("totalUsers", totalUsers);
        data.put("activeUsers", activeUsers);
        data.put("totalFiles", totalFiles);
        data.put("totalFolders", totalFolders);
        data.put("totalStorageUsed", totalStorageUsed);
        data.put("totalShares", totalShares);
        data.put("recentUsers", recentUsers.getContent());
        data.put("topUsersByStorage", topUsersByStorage);

        return ApiResponse.builder()
                .success(true)
                .message("System statistics retrieved successfully")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // Helper methods

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}