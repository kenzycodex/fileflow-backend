package com.fileflow.service.activity;

import com.fileflow.dto.response.common.PagedResponse;
import com.fileflow.exception.ResourceNotFoundException;
import com.fileflow.model.Activity;
import com.fileflow.model.User;
import com.fileflow.repository.ActivityRepository;
import com.fileflow.repository.UserRepository;
import com.fileflow.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityServiceImpl implements ActivityService {

    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;

    @Override
    public Activity logActivity(Long userId, String activityType, String itemType, Long itemId, String description) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        HttpServletRequest request = null;
        String ipAddress = "0.0.0.0";
        String deviceInfo = "Unknown";

        try {
            // Try to get request information if available
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                request = attributes.getRequest();
                ipAddress = getClientIp(request);
                deviceInfo = getUserAgent(request);
            }
        } catch (Exception e) {
            log.warn("Could not retrieve request information", e);
        }

        Activity activity = Activity.builder()
                .user(user)
                .activityType(activityType)
                .itemId(itemId)
                .itemType(itemType)
                .description(description)
                .ipAddress(ipAddress)
                .deviceInfo(deviceInfo)
                .createdAt(LocalDateTime.now())
                .build();

        return activityRepository.save(activity);
    }

    @Override
    public PagedResponse<Activity> getCurrentUserActivities(int page, int size) {
        UserPrincipal currentUser = getCurrentUserPrincipal();
        return getUserActivities(currentUser.getId(), page, size);
    }

    @Override
    public PagedResponse<Activity> getUserActivities(Long userId, int page, int size) {
        validatePageNumberAndSize(page, size);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Retrieve activities
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Activity> activities = activityRepository.findByUser(user, pageable);

        if (activities.getNumberOfElements() == 0) {
            return new PagedResponse<>(Collections.emptyList(), activities.getNumber(),
                    activities.getSize(), activities.getTotalElements(), activities.getTotalPages(), activities.isLast());
        }

        return new PagedResponse<>(activities.getContent(), activities.getNumber(),
                activities.getSize(), activities.getTotalElements(), activities.getTotalPages(), activities.isLast());
    }

    @Override
    public List<Activity> getRecentActivities(int limit) {
        UserPrincipal currentUser = getCurrentUserPrincipal();
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));

        Pageable pageable = PageRequest.of(0, limit, Sort.Direction.DESC, "createdAt");
        return activityRepository.findByUser(user, pageable).getContent();
    }

    @Override
    @Transactional
    public int deleteOldActivities(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        List<Activity> oldActivities = activityRepository.findByCreatedAtBefore(cutoffDate);

        if (!oldActivities.isEmpty()) {
            activityRepository.deleteAll(oldActivities);
            log.info("Deleted {} old activities", oldActivities.size());
            return oldActivities.size();
        }

        return 0;
    }

    private UserPrincipal getCurrentUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (UserPrincipal) authentication.getPrincipal();
    }

    private void validatePageNumberAndSize(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be less than zero.");
        }

        if (size < 1) {
            throw new IllegalArgumentException("Page size must not be less than one.");
        }

        if (size > 100) {
            throw new IllegalArgumentException("Page size must not be greater than 100.");
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }

        // In case of multiple proxies, first address is the client's
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.substring(0, ipAddress.indexOf(",")).trim();
        }

        return ipAddress;
    }

    private String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }
}
