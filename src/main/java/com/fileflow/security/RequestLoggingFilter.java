package com.fileflow.security;

import com.fileflow.util.SecurityUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

/**
 * Filter to log all incoming requests for security purposes
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@Slf4j
@RequiredArgsConstructor
public class RequestLoggingFilter implements Filter {

    private final SecurityUtils securityUtils;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Generate trace ID for request tracking
        String traceId = UUID.randomUUID().toString();
        httpResponse.setHeader("X-Trace-Id", traceId);

        // Get client info
        String ipAddress = securityUtils.getClientIpAddress(httpRequest);
        String userAgent = securityUtils.getUserAgent(httpRequest);
        String requestURI = httpRequest.getRequestURI();
        String queryString = httpRequest.getQueryString();
        String method = httpRequest.getMethod();

        // Skip logging for static resources
        if (isStaticResource(requestURI)) {
            chain.doFilter(request, response);
            return;
        }

        // Create wrappers to cache request/response content
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(httpRequest);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(httpResponse);

        long startTime = System.currentTimeMillis();

        try {
            // Log request info
            log.info("REQUEST [{}] {} {} from IP: {} UA: {} TraceId: {}",
                    method, requestURI, queryString != null ? "?" + queryString : "",
                    ipAddress, userAgent, traceId);

            // Proceed with the filter chain
            chain.doFilter(requestWrapper, responseWrapper);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = responseWrapper.getStatus();

            // Log response info
            log.info("RESPONSE [{}] {} {} - {} took {}ms TraceId: {}",
                    method, requestURI, queryString != null ? "?" + queryString : "",
                    status, duration, traceId);

            // Log detailed request/response only for non-200 responses
            if (status < 200 || status >= 300) {
                logDetailedRequestResponse(requestWrapper, responseWrapper, status, traceId);
            }

            // Copy content back to the original response
            responseWrapper.copyBodyToResponse();
        }
    }

    /**
     * Log detailed request and response content for debugging
     */
    private void logDetailedRequestResponse(ContentCachingRequestWrapper request,
                                            ContentCachingResponseWrapper response,
                                            int status, String traceId) throws UnsupportedEncodingException {
        // Only log detailed info for non-success responses
        if (status >= 400) {
            byte[] requestContent = request.getContentAsByteArray();
            String requestBody = new String(requestContent, request.getCharacterEncoding());

            byte[] responseContent = response.getContentAsByteArray();
            String responseBody = new String(responseContent, response.getCharacterEncoding());

            // Truncate bodies if too long
            if (requestBody.length() > 1000) {
                requestBody = requestBody.substring(0, 1000) + "... [truncated]";
            }

            if (responseBody.length() > 1000) {
                responseBody = responseBody.substring(0, 1000) + "... [truncated]";
            }

            log.debug("REQUEST BODY [{}]: {}", traceId, requestBody);
            log.debug("RESPONSE BODY [{}]: {}", traceId, responseBody);
        }
    }

    /**
     * Check if the request is for a static resource
     */
    private boolean isStaticResource(String path) {
        return path.startsWith("/static/") ||
                path.startsWith("/assets/") ||
                path.startsWith("/css/") ||
                path.startsWith("/js/") ||
                path.startsWith("/images/") ||
                path.startsWith("/fonts/") ||
                path.endsWith(".css") ||
                path.endsWith(".js") ||
                path.endsWith(".png") ||
                path.endsWith(".jpg") ||
                path.endsWith(".jpeg") ||
                path.endsWith(".gif") ||
                path.endsWith(".ico") ||
                path.endsWith(".svg") ||
                path.endsWith(".woff") ||
                path.endsWith(".woff2") ||
                path.endsWith(".ttf");
    }
}