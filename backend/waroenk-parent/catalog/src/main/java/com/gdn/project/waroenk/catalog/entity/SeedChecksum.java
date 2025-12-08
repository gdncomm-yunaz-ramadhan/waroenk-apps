package com.gdn.project.waroenk.catalog.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Entity to track processed seed files by their checksum.
 * Used to prevent re-execution of seed migrations for already processed files.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "seed_checksums")
public class SeedChecksum {
  @Id
  private String id;

  @Indexed(unique = true)
  private String fileName;

  private String checksum;

  private Integer recordCount;

  @CreatedDate
  private Instant processedAt;
}











