package com.fileflow.security;

import com.fileflow.config.JwtConfig;
import com.fileflow.service.auth.JwtService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

    private final JwtConfig jwtConfig;
    private final JwtService jwtService;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generate a JWT token from authentication with default expiration
     */
    public String generateToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return generateToken(userPrincipal.getId());
    }

    /**
     * Generate a JWT token from authentication with custom expiration
     */
    public String generateTokenWithCustomExpiration(Authentication authentication, long expirationMs) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return generateToken(userPrincipal.getId(), expirationMs);
    }

    /**
     * Generate a JWT token from user ID with default expiration
     */
    public String generateToken(Long userId) {
        return generateToken(userId, jwtConfig.getExpiration());
    }

    /**
     * Generate a JWT token from user ID with custom expiration
     */
    public String generateToken(Long userId, long expirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);
        String tokenId = generateTokenId();

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userId.toString());
        claims.put("jti", tokenId); // Add a unique token ID
        claims.put("iat", now.getTime() / 1000); // Issued at time
        claims.put("type", "access"); // Token type

        // Add custom expiration flag if this is a long-lived token
        if (expirationMs > (24 * 60 * 60 * 1000)) { // > 24 hours
            claims.put("remember_me", true);
        }

        String token = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();

        // Store the latest access token with custom expiration
        jwtService.saveAccessToken(userId, token, expirationMs);

        return token;
    }

    /**
     * Generate a refresh token with a family ID for tracking with default expiration
     */
    public String generateRefreshToken(Long userId) {
        return generateRefreshTokenWithCustomExpiration(userId, jwtConfig.getRefreshExpiration());
    }

    /**
     * Generate a refresh token with a family ID for tracking with custom expiration
     */
    public String generateRefreshTokenWithCustomExpiration(Long userId, long expirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);
        String tokenId = generateTokenId();

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userId.toString());
        claims.put("jti", tokenId); // Add a unique token ID
        claims.put("iat", now.getTime() / 1000); // Issued at time
        claims.put("type", "refresh"); // Token type
        claims.put("family", tokenId); // Family ID for tracking reuse

        // Add custom expiration flag if this is a long-lived token
        if (expirationMs > (24 * 60 * 60 * 1000)) { // > 24 hours
            claims.put("remember_me", true);
        }

        String token = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();

        // Store refresh token with custom expiration
        jwtService.saveRefreshToken(userId, token, expirationMs);

        return token;
    }

    /**
     * Extract user ID from JWT
     */
    public Long getUserIdFromJWT(String token) {
        Claims claims = parseClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * Extract token ID (jti) from JWT
     */
    public String getTokenIdFromJWT(String token) {
        Claims claims = parseClaims(token);
        return claims.getId();
    }

    /**
     * Extract token family ID from JWT
     */
    public String getTokenFamilyFromJWT(String token) {
        Claims claims = parseClaims(token);
        return (String) claims.get("family");
    }

    /**
     * Extract token type from JWT
     */
    public String getTokenTypeFromJWT(String token) {
        Claims claims = parseClaims(token);
        return (String) claims.get("type");
    }

    /**
     * Check if token has remember me flag
     */
    public boolean isRememberMeToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return Boolean.TRUE.equals(claims.get("remember_me"));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Parse claims from token
     */
    public Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            // For expired tokens, still return the claims for information extraction
            return e.getClaims();
        }
    }

    /**
     * Validate a JWT token
     */
    public boolean validateToken(String authToken) {
        try {
            // First check if token is blacklisted
            if (jwtService.isTokenBlacklisted(authToken)) {
                log.warn("Attempt to use blacklisted token");
                return false;
            }

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(authToken)
                    .getBody();

            // Check if it's a refresh token and its family is expired
            String tokenType = (String) claims.get("type");
            if ("refresh".equals(tokenType)) {
                String family = (String) claims.get("family");
                if (family != null && jwtService.isTokenFamilyExpired(family)) {
                    log.warn("Refresh token family expired");
                    return false;
                }
            }

            return true;
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty");
        }
        return false;
    }

    /**
     * Get signing key from secret
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate a secure random token ID
     */
    private String generateTokenId() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}