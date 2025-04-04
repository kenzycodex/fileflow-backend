package com.fileflow.security;

import com.fileflow.config.JwtConfig;
import com.fileflow.service.auth.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtConfig jwtConfig;
    private final JwtService jwtService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // List of paths that should be excluded from JWT authentication
    private final List<String> excludedPaths = Arrays.asList(
            "/api/v1/auth/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**",
            "/configuration/**",
            "/api/v1/shares/links/**",
            "/api/v1/health",
            "/api/v1/users/check-username",
            "/api/v1/users/check-email",
            "/",
            "/favicon.ico",
            "/static/**",
            "/*.png", "/*.gif", "/*.svg", "/*.jpg", "/*.html", "/*.css", "/*.js"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // Check if path matches any of the excluded patterns
        return excludedPaths.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {
                // First check if token is blacklisted
                if (jwtService.isTokenBlacklisted(jwt)) {
                    log.warn("Attempt to use a blacklisted token");
                    filterChain.doFilter(request, response);
                    return;
                }

                // Validate token
                if (tokenProvider.validateToken(jwt)) {
                    Long userId = tokenProvider.getUserIdFromJWT(jwt);
                    String tokenType = tokenProvider.getTokenTypeFromJWT(jwt);

                    // Only validate access tokens against user sessions
                    if ("access".equals(tokenType)) {
                        String latestToken = jwtService.getLatestAccessToken(userId);

                        // Check if token is current
                        if (latestToken == null || !jwt.equals(latestToken)) {
                            log.warn("Attempt to use an invalidated access token for user ID: {}", userId);
                            filterChain.doFilter(request, response);
                            return;
                        }

                        // Load user and set authentication
                        UserDetails userDetails = customUserDetailsService.loadUserById(userId);
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.debug("Set authentication for user ID: {}", userId);
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(jwtConfig.getHeader());
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(jwtConfig.getPrefix() + " ")) {
            return bearerToken.substring(jwtConfig.getPrefix().length() + 1);
        }
        return null;
    }
}