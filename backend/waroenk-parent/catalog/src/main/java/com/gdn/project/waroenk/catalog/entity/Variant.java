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
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "variants")
public class Variant {
  @Id
  private String id;

  @Indexed
  private String sku;

  @Indexed(unique = true)
  private String subSku;

  @TextIndexed
  private String title;

  private Double price;

  @Builder.Default
  private Boolean isDefault = false;

  private Map<String, Object> attributes;

  private String thumbnail;

  private List<VariantMedia> media;

  @CreatedDate
  private Instant createdAt;

  @LastModifiedDate
  private Instant updatedAt;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class VariantMedia {
    private String url;
    private String type;
    private Integer sortOrder;
    private String altText;
  }
}
