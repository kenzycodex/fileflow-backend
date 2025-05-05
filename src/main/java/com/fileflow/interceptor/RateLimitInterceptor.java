package com.fileflow.interceptor;

import com.fileflow.annotation.RateLimit;
import com.fileflow.exception.TooManyRequestsException;
import com.fileflow.security.UserPrincipal;
import com.fileflow.service.security.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor to apply rate limiting to API endpoints
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true; // Skip if not a controller method
        }

        // Check for rate limit annotation on method
        RateLimit rateLimit = handlerMethod.getMethod().getAnnotation(RateLimit.class);
        if (rateLimit == null) {
            // Check for rate limit annotation on class
            rateLimit = handlerMethod.getBeanType().getAnnotation(RateLimit.class);
            if (rateLimit == null) {
                return true; // No rate limit annotation found
            }
        }

        // Determine the rate limit key
        String key = resolveKey(request, rateLimit.keyResolver());
        if (key == null) {
            return true; // Unable to determine key, skip rate limiting
        }

        // Apply rate limiting based on type
        boolean allowed = switch (rateLimit.type()) {
            case API -> rateLimiterService.checkRateLimit(key);
            case LOGIN -> rateLimiterService.checkLoginRateLimit(key);
            case SIGNUP -> rateLimiterService.checkSignupRateLimit(key);
            case PASSWORD -> rateLimiterService.checkPasswordResetRateLimit(key);
            case TOKEN -> {
                // Extract token from request (example implementation - adjust as needed)
                String token = request.getParameter("token");
                if (token != null) {
                    yield rateLimiterService.checkResetTokenRateLimit(token);
                }
                yield true;
            }
        };

        if (!allowed) {
            throw new TooManyRequestsException("Rate limit exceeded. Please try again later.", 60L);
        }

        return true;
    }

    /**
     * Resolve the rate limit key based on the specified resolver
     */
    private String resolveKey(HttpServletRequest request, RateLimit.KeyResolver keyResolver) {
        return switch (keyResolver) {
            case IP -> getClientIp(request);
            case USER -> {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
                    yield userPrincipal.getId().toString();
                }
                yield getClientIp(request); // Fall back to IP if user not authenticated
            }
            case CUSTOM -> {
                // For custom key resolvers, could implement a strategy pattern
                // or use a specific request parameter
                yield request.getRequestURI() + ":" + getClientIp(request);
            }
        };
    }

    /**
     * Get client IP address, handling proxies
     */
    private String getClientIp(HttpServletRequest request) {
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("Proxy-Client-IP");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("WL-Proxy-Client-IP");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getRemoteAddr();
        }

        // If multiple IPs are present, take the first one (client IP)
        if (clientIp != null && clientIp.contains(",")) {
            clientIp = clientIp.split(",")[0].trim();
        }

        return clientIp;
    }
}