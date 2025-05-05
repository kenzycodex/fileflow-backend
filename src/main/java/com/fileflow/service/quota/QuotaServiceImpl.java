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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuotaServiceImpl implements QuotaService {

    private final UserRepository userRepository;
    private final QuotaExtensionRepository quotaExtensionRepository;
    private final RedisTemplate<String, String> redisTemplate;

    // Redis key prefix for quota reservations
    private static final String QUOTA_RESERVATION_PREFIX = "quota:reservation:";
    // Expiry time for reservations (in seconds)
    private static final int RESERVATION_EXPIRY_SECONDS = 3600; // 1 hour

    @Override
    @Transactional
    public boolean checkAndReserveQuota(Long userId, long size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Get current storage usage
        long currentUsage = user.getStorageUsed();

        // Add reserved storage from Redis (if any)
        Long reserved = getReservedStorage(userId);
        currentUsage += reserved;

        // Get user's total quota (base + extensions)
        long totalQuota = calculateTotalQuota(user);

        // Check if enough space
        if (currentUsage + size > totalQuota) {
            return false;
        }

        // Reserve storage in Redis
        reserveStorage(userId, reserved + size);

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
        releaseQuotaReservation(userId, size);
    }

    @Override
    public void releaseQuotaReservation(Long userId, long size) {
        String key = QUOTA_RESERVATION_PREFIX + userId;

        try {
            // Get current reserved value
            String reservedStr = redisTemplate.opsForValue().get(key);
            long reserved = 0;

            if (reservedStr != null) {
                reserved = Long.parseLong(reservedStr);
            }

            // Calculate new value (never below zero)
            long newReserved = Math.max(0, reserved - size);

            if (newReserved > 0) {
                // Update with new value
                redisTemplate.opsForValue().set(key, String.valueOf(newReserved), RESERVATION_EXPIRY_SECONDS, TimeUnit.SECONDS);
            } else {
                // Remove key if zero
                redisTemplate.delete(key);
            }

            log.debug("Released quota reservation for user {}: {} bytes, remaining reservation: {}", userId, size, newReserved);
        } catch (Exception e) {
            log.error("Error releasing quota reservation for user {}: {}", userId, e.getMessage(), e);
        }
    }

    @Override
    public void cancelReservedQuota(Long userId, long size) {
        // Delegate to releaseQuotaReservation for consistent implementation
        releaseQuotaReservation(userId, size);
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

        // Get reserved storage
        long reservedStorage = getReservedStorage(userId);

        // Calculate usage percentage
        double usagePercentage = (double) user.getStorageUsed() / totalQuota * 100;

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("baseQuota", user.getStorageQuota());
        data.put("quotaExtensions", getActiveQuotaExtensions(user));
        data.put("totalQuota", totalQuota);
        data.put("usedStorage", user.getStorageUsed());
        data.put("reservedStorage", reservedStorage);
        data.put("availableStorage", totalQuota - user.getStorageUsed() - reservedStorage);
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

    /**
     * Calculate total quota for a user
     */
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

    /**
     * Get active quota extensions for a user
     */
    private List<QuotaExtension> getActiveQuotaExtensions(User user) {
        LocalDateTime now = LocalDateTime.now();
        return quotaExtensionRepository.findByUserAndExpiryDateAfter(user, now);
    }

    /**
     * Get reserved storage for a user from Redis
     */
    private long getReservedStorage(Long userId) {
        String key = QUOTA_RESERVATION_PREFIX + userId;
        String reservedStr = redisTemplate.opsForValue().get(key);

        if (reservedStr != null) {
            try {
                return Long.parseLong(reservedStr);
            } catch (NumberFormatException e) {
                log.error("Invalid reserved storage value in Redis for user {}: {}", userId, reservedStr);
            }
        }

        return 0L;
    }

    /**
     * Reserve storage for a user in Redis
     */
    private void reserveStorage(Long userId, long amount) {
        String key = QUOTA_RESERVATION_PREFIX + userId;
        redisTemplate.opsForValue().set(key, String.valueOf(amount), RESERVATION_EXPIRY_SECONDS, TimeUnit.SECONDS);
        log.debug("Reserved {} bytes for user {}", amount, userId);
    }
}