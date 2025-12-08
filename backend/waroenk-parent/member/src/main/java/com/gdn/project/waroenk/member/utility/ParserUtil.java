package com.gdn.project.waroenk.member.utility;


import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ParserUtil {

  private ParserUtil() {

  }

  /**
   * Decode Base64 string (supports both standard and URL-safe encoding)
   */
  public static String decodeBase64(String input) {
    if (StringUtils.isBlank(input)) {
      return null;
    }
    try {
      // Try URL-safe decoder first (no padding)
      Base64.Decoder decoder = Base64.getUrlDecoder();
      byte[] decodedBytes = decoder.decode(input.getBytes(StandardCharsets.UTF_8));
      return new String(decodedBytes, StandardCharsets.UTF_8);
    } catch (IllegalArgumentException e) {
      // Fallback to standard decoder for backward compatibility
      try {
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] decodedBytes = decoder.decode(input.getBytes(StandardCharsets.UTF_8));
        return new String(decodedBytes, StandardCharsets.UTF_8);
      } catch (IllegalArgumentException e2) {
        return null;
      }
    }
  }

  /**
   * Encode string to URL-safe Base64 (no padding, safe for URL query params)
   */
  public static String encodeBase64(String input) {
    if (StringUtils.isBlank(input)) {
      return null;
    }
    // Use URL-safe encoder without padding to avoid URL encoding issues
    Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
    byte[] encodedBytes = encoder.encode(input.getBytes(StandardCharsets.UTF_8));
    return new String(encodedBytes, StandardCharsets.UTF_8);
  }
}
