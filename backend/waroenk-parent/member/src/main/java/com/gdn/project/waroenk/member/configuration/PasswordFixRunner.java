package com.gdn.project.waroenk.member.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Fixes test user passwords on startup.
 * Only runs with "fix-passwords" profile.
 * 
 * Usage: mvn spring-boot:run -Dspring-boot.run.profiles=fix-passwords
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@Profile("fix-passwords")
public class PasswordFixRunner {

  private static final String TEST_PASSWORD = "Testing@123";

  private final JdbcTemplate jdbcTemplate;
  private final PasswordEncoder passwordEncoder;

  @Bean
  public CommandLineRunner fixPasswords() {
    return args -> {
      log.info("Starting password fix for test users...");
      
      // Generate correct BCrypt hash using app's encoder
      String correctHash = passwordEncoder.encode(TEST_PASSWORD);
      log.info("Generated BCrypt hash for '{}': {}", TEST_PASSWORD, correctHash);
      
      // Update all test users
      int updated = jdbcTemplate.update(
          "UPDATE users SET password_hash = ? WHERE email LIKE '%@testmail.com'",
          correctHash
      );
      
      log.info("✅ Fixed password for {} test users", updated);
      log.info("You can now login with password: {}", TEST_PASSWORD);
    };
  }
}












