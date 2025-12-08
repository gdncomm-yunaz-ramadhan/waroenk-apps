package com.gdn.project.waroenk.catalog.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "categories")
public class Category {
  @Id
  private String id;

  @TextIndexed
  private String name;

  @Indexed(unique = true)
  private String slug;

  private String parentId;

  private String iconUrl;

  @CreatedDate
  private Instant createdAt;

  @LastModifiedDate
  private Instant updatedAt;
}



