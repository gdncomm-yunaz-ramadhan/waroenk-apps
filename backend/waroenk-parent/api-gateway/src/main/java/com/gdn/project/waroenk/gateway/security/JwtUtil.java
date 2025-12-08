package com.gdn.project.waroenk.gateway.security;

import com.gdn.project.waroenk.gateway.config.GatewayProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final GatewayProperties gatewayProperties;
    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        String secret = gatewayProperties.getJwt().getSecret();
        if (secret != null && !secret.isBlank()) {
            // Try to decode as Base64 first, fallback to raw string
            try {
                byte[] keyBytes = Base64.getDecoder().decode(secret);
                secretKey = Keys.hmacShaKeyFor(keyBytes);
            } catch (IllegalArgumentException e) {
                secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            }
        } else {
            log.warn("JWT secret not configured, using default key (NOT SECURE FOR PRODUCTION)");
            secretKey = Keys.hmacShaKeyFor(
                    "default-secret-key-for-development-only-minimum-256-bits".getBytes(StandardCharsets.UTF_8));
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, claims -> {
            // Try to get email first (member service includes this)
            String email = claims.get("email", String.class);
            if (email != null && !email.isBlank()) {
                return email;
            }
            // Fallback to phone
            String phone = claims.get("phone", String.class);
            if (phone != null && !phone.isBlank()) {
                return phone;
            }
            // Final fallback to subject (which is user ID in member service)
            return claims.getSubject();
        });
    }

    public String extractUserId(String token) {
        return extractClaim(token, claims -> {
            // Try different claim keys for compatibility with member service
            // Member service uses "userId" (camelCase) and also sets subject to user ID
            String userId = claims.get("userId", String.class);
            if (userId != null && !userId.isBlank()) {
                return userId;
            }
            // Fallback to snake_case (for compatibility)
            userId = claims.get("user_id", String.class);
            if (userId != null && !userId.isBlank()) {
                return userId;
            }
            // Final fallback: use subject claim (member service sets this to user ID)
            return claims.getSubject();
        });
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return extractClaim(token, claims -> claims.get("roles", List.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return !isTokenExpired(token);
        } catch (ExpiredJwtException e) {
            log.debug("JWT token expired: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public boolean validateTokenWithClaims(String token, String expectedUserId) {
        try {
            if (!validateToken(token)) {
                return false;
            }
            String userId = extractUserId(token);
            return userId != null && userId.equals(expectedUserId);
        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}





