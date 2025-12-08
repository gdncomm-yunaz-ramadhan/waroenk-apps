package com.gdn.project.waroenk.member.utility;

import com.gdn.project.waroenk.member.entity.User;
import com.gdn.project.waroenk.member.service.SystemParameterService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class JwtUtil {

  private static final String ACCESS_TOKEN_EXPIRY_PARAM = "ACCESS_TOKEN_EXPIRY_MINUTES";
  private static final String REFRESH_TOKEN_EXPIRY_PARAM = "REFRESH_TOKEN_EXPIRY_HOURS";
  private static final String RESET_TOKEN_EXPIRY_PARAM = "RESET_TOKEN_EXPIRY_MINUTES";
  private static final String DEFAULT_ACCESS_TOKEN_EXPIRY = "30";
  private static final String DEFAULT_REFRESH_TOKEN_EXPIRY = "24";
  private static final String DEFAULT_RESET_TOKEN_EXPIRY = "15";
  private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

  private final SystemParameterService systemParameterService;
  private final CacheUtil<String> stringCacheUtil;

  @Value("${jwt.secret:c2VjdXJlLXNlY3JldC1rZXktZm9yLWp3dC10b2tlbi1nZW5lcmF0aW9uLW1pbmltdW0tMjU2LWJpdHM=}")
  private String secretKey;

  public JwtUtil(SystemParameterService systemParameterService, CacheUtil<String> stringCacheUtil) {
    this.systemParameterService = systemParameterService;
    this.stringCacheUtil = stringCacheUtil;
  }

  private SecretKey getSigningKey() {
    byte[] keyBytes = Decoders.BASE64.decode(secretKey);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  public String generateAccessToken(User user) {
    int expiryMinutes = Integer.parseInt(
        systemParameterService.getVariableData(ACCESS_TOKEN_EXPIRY_PARAM, DEFAULT_ACCESS_TOKEN_EXPIRY));

    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", user.getId().toString());
    claims.put("email", user.getEmail());
    claims.put("phone", user.getPhoneNumber());

    return Jwts.builder()
        .claims(claims)
        .subject(user.getId().toString())
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + (long) expiryMinutes * 60 * 1000))
        .signWith(getSigningKey())
        .compact();
  }

  public String generateRefreshToken() {
    SecureRandom secureRandom = new SecureRandom();
    byte[] tokenBytes = new byte[64];
    secureRandom.nextBytes(tokenBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
  }

  public long getAccessTokenExpirySeconds() {
    int expiryMinutes = Integer.parseInt(
        systemParameterService.getVariableData(ACCESS_TOKEN_EXPIRY_PARAM, DEFAULT_ACCESS_TOKEN_EXPIRY));
    return (long) expiryMinutes * 60;
  }

  public long getRefreshTokenExpiryHours() {
    return Long.parseLong(
        systemParameterService.getVariableData(REFRESH_TOKEN_EXPIRY_PARAM, DEFAULT_REFRESH_TOKEN_EXPIRY));
  }

  public Claims extractClaims(String token) {
    return Jwts.parser()
        .verifyWith(getSigningKey())
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  public String extractUserId(String token) {
    return extractClaims(token).getSubject();
  }

  public boolean isTokenExpired(String token) {
    return extractClaims(token).getExpiration().before(new Date());
  }

  public boolean validateToken(String token, User user) {
    String userId = extractUserId(token);
    return userId.equals(user.getId().toString()) && !isTokenExpired(token) && !isTokenBlacklisted(token);
  }

  /**
   * Blacklist a token to invalidate it before expiration
   */
  public void blacklistToken(String token) {
    if (token == null || token.isEmpty()) {
      return;
    }
    try {
      Claims claims = extractClaims(token);
      Date expiration = claims.getExpiration();
      long ttlMillis = expiration.getTime() - System.currentTimeMillis();
      if (ttlMillis > 0) {
        String key = TOKEN_BLACKLIST_PREFIX + token;
        stringCacheUtil.putValue(key, "blacklisted", ttlMillis / 1000, TimeUnit.SECONDS);
        log.info("Token blacklisted successfully");
      }
    } catch (ExpiredJwtException e) {
      // Token already expired, no need to blacklist
      log.debug("Token already expired, skipping blacklist");
    } catch (Exception e) {
      log.warn("Failed to blacklist token: {}", e.getMessage());
    }
  }

  /**
   * Check if a token is blacklisted
   */
  public boolean isTokenBlacklisted(String token) {
    if (token == null || token.isEmpty()) {
      return false;
    }
    String key = TOKEN_BLACKLIST_PREFIX + token;
    String value = stringCacheUtil.getValue(key);
    return value != null;
  }

  /**
   * Generate a password reset token
   */
  public String generateResetToken() {
    SecureRandom secureRandom = new SecureRandom();
    byte[] tokenBytes = new byte[32];
    secureRandom.nextBytes(tokenBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
  }

  /**
   * Get reset token expiry in seconds
   */
  public long getResetTokenExpirySeconds() {
    int expiryMinutes = Integer.parseInt(
        systemParameterService.getVariableData(RESET_TOKEN_EXPIRY_PARAM, DEFAULT_RESET_TOKEN_EXPIRY));
    return (long) expiryMinutes * 60;
  }

  /**
   * Get reset token expiry in minutes
   */
  public int getResetTokenExpiryMinutes() {
    return Integer.parseInt(
        systemParameterService.getVariableData(RESET_TOKEN_EXPIRY_PARAM, DEFAULT_RESET_TOKEN_EXPIRY));
  }
}





