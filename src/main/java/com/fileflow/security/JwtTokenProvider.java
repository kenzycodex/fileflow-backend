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
     * Generate a JWT token from authentication
     */
    public String generateToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return generateToken(userPrincipal.getId());
    }

    /**
     * Generate a JWT token from user ID
     */
    public String generateToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getExpiration());
        String tokenId = generateTokenId();

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userId.toString());
        claims.put("jti", tokenId); // Add a unique token ID
        claims.put("iat", now.getTime() / 1000); // Issued at time
        claims.put("type", "access"); // Token type

        String token = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();

        // Store the latest access token
        jwtService.saveAccessToken(userId, token);

        return token;
    }

    /**
     * Generate a refresh token with a family ID for tracking
     */
    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getRefreshExpiration());
        String tokenId = generateTokenId();

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userId.toString());
        claims.put("jti", tokenId); // Add a unique token ID
        claims.put("iat", now.getTime() / 1000); // Issued at time
        claims.put("type", "refresh"); // Token type
        claims.put("family", tokenId); // Family ID for tracking reuse

        String token = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();

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
     * Parse claims from token
     */
    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
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