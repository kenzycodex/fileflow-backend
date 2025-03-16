package com.fileflow.service.quota;

import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.exception.BadRequestException;
import com.fileflow.exception.ResourceNotFoundException;
import com.fileflow.model.QuotaExtension;
import com.fileflow.model.User;
import com.fileflow.repository.QuotaExtensionRepository;
import com.fileflow.repository.UserRepository;
import com.fileflow.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuotaServiceImpl implements QuotaService {

    private final UserRepository userRepository;
    private final QuotaExtensionRepository quotaExtensionRepository;

    // Map to track reserved but not confirmed storage
    // In a production environment, this should be stored in Redis or similar
    private final Map<Long, Long> reservedStorage = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public boolean checkAndReserveQuota(Long userId, long size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Get current storage usage (including reserved storage)
        long currentUsage = user.getStorageUsed();

        // Add reserved storage (if any)
        Long reserved = reservedStorage.getOrDefault(userId, 0L);
        currentUsage += reserved;

        // Get user's total quota (base + extensions)
        long totalQuota = calculateTotalQuota(user);

        // Check if enough space
        if (currentUsage + size > totalQuota) {
            return false;
        }

        // Reserve storage
        reservedStorage.put(userId, reserved + size);

        return true;
    }

    @Override
    @Transactional
    public void confirmQuotaUsage(Long userId, long size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Update user's storage usage
        user.setStorageUsed(user.getStorageUsed() + size);
        userRepository.save(user);

        // Remove from reserved storage
        Long reserved = reservedStorage.getOrDefault(userId, 0L);
        if (reserved >= size) {
            reservedStorage.put(userId, reserved - size);
        } else {
            reservedStorage.remove(userId);
        }
    }

    @Override
    public void cancelReservedQuota(Long userId, long size) {
        // Remove from reserved storage
        Long reserved = reservedStorage.getOrDefault(userId, 0L);
        if (reserved >= size) {
            reservedStorage.put(userId, reserved - size);
        } else {
            reservedStorage.remove(userId);
        }
    }

    @Override
    @Transactional
    public void updateStorageUsed(Long userId, long additionalSize) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setStorageUsed(user.getStorageUsed() + additionalSize);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void releaseStorage(Long userId, long size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Calculate new storage used (never negative)
        long newStorageUsed = Math.max(0, user.getStorageUsed() - size);

        user.setStorageUsed(newStorageUsed);
        userRepository.save(user);
    }

    @Override
    public ApiResponse getStorageUsage(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Calculate total quota
        long totalQuota = calculateTotalQuota(user);

        // Calculate usage percentage
        double usagePercentage = (double) user.getStorageUsed() / totalQuota * 100;

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("baseQuota", user.getStorageQuota());
        data.put("quotaExtensions", getActiveQuotaExtensions(user));
        data.put("totalQuota", totalQuota);
        data.put("usedStorage", user.getStorageUsed());
        data.put("availableStorage", totalQuota - user.getStorageUsed());
        data.put("usagePercentage", Math.round(usagePercentage * 100) / 100.0); // Round to 2 decimal places

        return ApiResponse.builder()
                .success(true)
                .message("Storage usage retrieved successfully")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public ApiResponse updateQuota(Long userId, long newQuota) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Validate new quota
        if (newQuota < Constants.MIN_STORAGE_QUOTA) {
            throw new BadRequestException("Quota cannot be less than " +
                    (Constants.MIN_STORAGE_QUOTA / (1024L * 1024L * 1024L)) + "GB");
        }

        // Update quota
        user.setStorageQuota(newQuota);
        userRepository.save(user);

        return ApiResponse.builder()
                .success(true)
                .message("Storage quota updated successfully")
                .data(Map.of(
                        "userId", user.getId(),
                        "newQuota", newQuota
                ))
                .timestamp(LocalDateTime.now())
                .build();
    }

    // Helper methods

    private long calculateTotalQuota(User user) {
        // Base quota
        long totalQuota = user.getStorageQuota();

        // Add active quota extensions
        List<QuotaExtension> activeExtensions = getActiveQuotaExtensions(user);
        for (QuotaExtension extension : activeExtensions) {
            totalQuota += extension.getAdditionalSpace();
        }

        return totalQuota;
    }

    private List<QuotaExtension> getActiveQuotaExtensions(User user) {
        LocalDateTime now = LocalDateTime.now();
        return quotaExtensionRepository.findByUserAndExpiryDateAfter(user, now);
    }
}