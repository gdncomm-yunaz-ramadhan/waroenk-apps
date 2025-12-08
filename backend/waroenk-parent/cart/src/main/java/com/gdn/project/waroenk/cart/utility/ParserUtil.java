package com.gdn.project.waroenk.cart.utility;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ParserUtil {
    
    /**
     * Encode string to URL-safe Base64 (no padding, safe for URL query params)
     */
    public static String encodeBase64(String input) {
        if (input == null) {
            return null;
        }
        // Use URL-safe encoder without padding to avoid URL encoding issues
        return Base64.getUrlEncoder().withoutPadding().encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decode Base64 string (supports both standard and URL-safe encoding)
     */
    public static String decodeBase64(String input) {
        if (input == null) {
            return null;
        }
        try {
            // Try URL-safe decoder first
            return new String(Base64.getUrlDecoder().decode(input), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            // Fallback to standard decoder for backward compatibility
            try {
                return new String(Base64.getDecoder().decode(input), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e2) {
                return null;
            }
        }
    }
}
