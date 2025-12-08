package com.gdn.project.waroenk.member.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility to generate BCrypt password hashes for seeding.
 * Run this to get the correct hash for your migrations.
 */
public class PasswordHashGenerator {
  
  public static void main(String[] args) {
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
    
    String password = "Testing@123";
    String hash = encoder.encode(password);
    
    System.out.println("=".repeat(60));
    System.out.println("Password: " + password);
    System.out.println("BCrypt Hash (strength 12): " + hash);
    System.out.println("=".repeat(60));
    
    // Verify it works
    boolean matches = encoder.matches(password, hash);
    System.out.println("Verification: " + (matches ? "✅ PASS" : "❌ FAIL"));
    
    // Also test the hash from migration
    String migrationHash = "$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.GA1Tdrdysr5zLe";
    boolean migrationMatches = encoder.matches(password, migrationHash);
    System.out.println("Migration hash verification: " + (migrationMatches ? "✅ PASS" : "❌ FAIL"));
  }
}












