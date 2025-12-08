package com.gdn.project.waroenk.catalog.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "system_parameters")
public class SystemParameter {
  @Id
  private String id;

  @Indexed(unique = true)
  private String variable;

  private String data;

  private String description;

  @CreatedDate
  private Instant createdAt;

  @LastModifiedDate
  private Instant updatedAt;
}












