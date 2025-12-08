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
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "products")
public class Product {
  @Id
  private String id;

  @TextIndexed
  private String title;

  @Indexed(unique = true)
  private String sku;

  @Indexed
  private String merchantCode;

  @Indexed
  private String categoryId;

  @Indexed
  private String brandId;

  private ProductSummary summary;

  private String detailRef;

  @CreatedDate
  private Instant createdAt;

  @LastModifiedDate
  private Instant updatedAt;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class ProductSummary {
    private String shortDescription;
    private List<String> tags;
  }
}
