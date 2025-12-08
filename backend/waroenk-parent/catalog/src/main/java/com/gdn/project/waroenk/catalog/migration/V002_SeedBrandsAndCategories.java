package com.gdn.project.waroenk.catalog.migration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdn.project.waroenk.catalog.entity.Category;
import com.gdn.project.waroenk.catalog.entity.SeedChecksum;
import com.github.cloudyrock.mongock.ChangeLog;
import com.github.cloudyrock.mongock.ChangeSet;
import com.github.cloudyrock.mongock.driver.mongodb.springdata.v3.decorator.impl.MongockTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Migration V002: Seed initial categories data.
 * 
 * Note: Brands and Merchants are now seeded via external script due to large file sizes.
 * Only categories are included in the JAR as they are small (~9KB).
 * 
 * Uses checksum validation to prevent re-seeding already processed files.
 * Data is loaded from JSON files in resources/seed-data/
 */
@Slf4j
@ChangeLog(order = "002")
public class V002_SeedBrandsAndCategories {

  private static final String CATEGORIES_FILE = "seed-data/categories.json";
  private final ObjectMapper objectMapper = new ObjectMapper();

  @ChangeSet(order = "001", id = "seedCategories", author = "system")
  public void seedCategories(MongockTemplate mongockTemplate) {
    log.info("Starting category seeding...");
    
    try {
      // Read file content
      ClassPathResource resource = new ClassPathResource(CATEGORIES_FILE);
      if (!resource.exists()) {
        log.warn("Categories seed file not found: {}. Skipping category seeding.", CATEGORIES_FILE);
        return;
      }

      String content;
      try (InputStream is = resource.getInputStream()) {
        content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      }
      
      // Calculate checksum
      String checksum = calculateChecksum(content);
      
      // Check if already processed
      if (isAlreadyProcessed(mongockTemplate, CATEGORIES_FILE, checksum)) {
        log.info("Categories file already processed with same checksum. Skipping.");
        return;
      }
      
      // Parse and insert categories
      List<Category> categories = objectMapper.readValue(content, new TypeReference<List<Category>>() {});
      
      // Set timestamps for all categories
      Instant now = Instant.now();
      categories.forEach(category -> {
        if (category.getId() == null) {
          category.setId(UUID.randomUUID().toString());
        }
        category.setCreatedAt(now);
        category.setUpdatedAt(now);
      });
      
      // Clear existing categories and insert new ones
      mongockTemplate.dropCollection(Category.class);
      mongockTemplate.insertAll(categories);
      
      // Record the checksum
      recordChecksum(mongockTemplate, CATEGORIES_FILE, checksum, categories.size());
      
      log.info("Successfully seeded {} categories", categories.size());
      
    } catch (IOException e) {
      log.error("Failed to seed categories: {}", e.getMessage(), e);
    }
  }

  private String calculateChecksum(String content) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not available", e);
    }
  }

  private boolean isAlreadyProcessed(MongockTemplate mongockTemplate, String fileName, String checksum) {
    Query query = Query.query(
        Criteria.where("fileName").is(fileName)
            .and("checksum").is(checksum)
    );
    return mongockTemplate.exists(query, SeedChecksum.class);
  }

  private void recordChecksum(MongockTemplate mongockTemplate, String fileName, String checksum, int recordCount) {
    // Remove old checksum record for this file if exists
    Query deleteQuery = Query.query(Criteria.where("fileName").is(fileName));
    mongockTemplate.remove(deleteQuery, SeedChecksum.class);
    
    // Insert new checksum record
    SeedChecksum seedChecksum = SeedChecksum.builder()
        .id(UUID.randomUUID().toString())
        .fileName(fileName)
        .checksum(checksum)
        .recordCount(recordCount)
        .processedAt(Instant.now())
        .build();
    mongockTemplate.insert(seedChecksum);
  }
}
