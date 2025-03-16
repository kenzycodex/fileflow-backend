package com.fileflow.service.statistics;

import com.fileflow.dto.response.statistics.ActivityStatistics;
import com.fileflow.dto.response.statistics.FileTypeStatistics;
import com.fileflow.dto.response.statistics.StorageStatistics;
import com.fileflow.dto.response.statistics.SystemStatistics;
import com.fileflow.exception.ForbiddenException;
import com.fileflow.exception.ResourceNotFoundException;
import com.fileflow.model.Activity;
import com.fileflow.model.File;
import com.fileflow.model.User;
import com.fileflow.repository.ActivityRepository;
import com.fileflow.repository.FileRepository;
import com.fileflow.repository.FolderRepository;
import com.fileflow.repository.UserRepository;
import com.fileflow.security.UserPrincipal;
import com.fileflow.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
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
    private final ActivityRepository activityRepository;

    @Override
    @Cacheable(value = "userStorage", key = "#root.methodName + '_' + @userRepository.getCurrentUserId()")
    public StorageStatistics getStorageStatistics() {
        User currentUser = getCurrentUser();
        return getStorageStatisticsForUser(currentUser.getId());
    }

    @Override
    @Cacheable(value = "userStorage", key = "#userId")
    public StorageStatistics getStorageStatisticsForUser(Long userId) {
        // Check if admin or if requesting own statistics
        User currentUser = getCurrentUser();
        if (!currentUser.getId().equals(userId) && !isAdmin()) {
            throw new ForbiddenException("You are not authorized to access this user's storage statistics");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Calculate quota usage
        long totalQuota = user.getStorageQuota();
        long usedStorage = user.getStorageUsed();
        double usagePercentage = (double) usedStorage / totalQuota * 100;

        // Get file and folder counts
        long fileCount = fileRepository.countByUserAndIsDeletedFalse(user);
        long folderCount = folderRepository.countByUserAndIsDeletedFalse(user);

        // Get deleted item counts
        long deletedFileCount = fileRepository.countByUserAndIsDeletedTrue(user);
        long deletedFolderCount = folderRepository.countByUserAndIsDeletedTrue(user);

        // Calculate average file size
        double averageFileSize = fileCount > 0 ? (double) usedStorage / fileCount : 0;

        return StorageStatistics.builder()
                .userId(userId)
                .username(user.getUsername())
                .totalQuota(totalQuota)
                .usedStorage(usedStorage)
                .availableStorage(totalQuota - usedStorage)
                .usagePercentage(Math.round(usagePercentage * 100) / 100.0)
                .fileCount(fileCount)
                .folderCount(folderCount)
                .deletedFileCount(deletedFileCount)
                .deletedFolderCount(deletedFolderCount)
                .averageFileSize(Math.round(averageFileSize * 100) / 100.0)
                .build();
    }

    @Override
    public ActivityStatistics getActivityStatistics(LocalDate startDate, LocalDate endDate) {
        User currentUser = getCurrentUser();

        // Convert dates to datetime
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // Get activities in date range
        List<Activity> activities = activityRepository.findByUserAndCreatedAtBetween(
                currentUser, startDateTime, endDateTime);

        // Count activities by type
        Map<String, Long> activityCounts = activities.stream()
                .collect(Collectors.groupingBy(
                        Activity::getActivityType,
                        Collectors.counting()
                ));

        // Count activities by date
        Map<LocalDate, Long> activityByDate = activities.stream()
                .collect(Collectors.groupingBy(
                        activity -> activity.getCreatedAt().toLocalDate(),
                        Collectors.counting()
                ));

        return ActivityStatistics.builder()
                .userId(currentUser.getId())
                .username(currentUser.getUsername())
                .startDate(startDate)
                .endDate(endDate)
                .totalActivities(activities.size())
                .activityCounts(activityCounts)
                .activityByDate(activityByDate)
                .build();
    }

    @Override
    @Cacheable(value = "fileTypeStats", key = "@userRepository.getCurrentUserId()")
    public FileTypeStatistics getFileTypeStatistics() {
        User currentUser = getCurrentUser();

        // Get user's files
        List<File> files = fileRepository.findByUserAndIsDeletedFalse(currentUser);

        // Count files by type
        Map<String, Long> fileCountsByType = files.stream()
                .collect(Collectors.groupingBy(
                        File::getFileType,
                        Collectors.counting()
                ));

        // Calculate storage by type
        Map<String, Long> storageByType = new HashMap<>();

        for (File file : files) {
            String fileType = file.getFileType();
            long size = file.getFileSize();

            storageByType.put(fileType, storageByType.getOrDefault(fileType, 0L) + size);
        }

        return FileTypeStatistics.builder()
                .userId(currentUser.getId())
                .username(currentUser.getUsername())
                .totalFiles(files.size())
                .fileCountsByType(fileCountsByType)
                .storageByType(storageByType)
                .build();
    }

    @Override
    public SystemStatistics getSystemStatistics() {
        // Only admin can access system statistics
        if (!isAdmin()) {
            throw new ForbiddenException("Only administrators can access system statistics");
        }

        // Get total users
        long totalUsers = userRepository.count();

        // Get active users (logged in within last 30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long activeUsers = userRepository.countByLastLoginAfter(thirtyDaysAgo);

        // Get total files and folders
        long totalFiles = fileRepository.count();
        long totalFolders = folderRepository.count();

        // Get total storage used
        long totalStorageUsed = userRepository.findAll().stream()
                .mapToLong(User::getStorageUsed)
                .sum();

        // Get activities in last 24 hours
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        long recentActivities = activityRepository.countByCreatedAtAfter(yesterday);

        return SystemStatistics.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .totalFiles(totalFiles)
                .totalFolders(totalFolders)
                .totalStorageUsed(totalStorageUsed)
                .activitiesLast24Hours(recentActivities)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userPrincipal.getId()));
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}