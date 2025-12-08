package com.gdn.project.waroenk.catalog.dto.product;

import java.time.Instant;
import java.util.List;

public record ProductResponseDto(
    String id,
    String title,
    String sku,
    String merchantCode,
    String categoryId,
    String brandId,
    ProductSummaryDto summary,
    String detailRef,
    Instant createdAt,
    Instant updatedAt
) {
  public record ProductSummaryDto(
      String shortDescription,
      List<String> tags
  ) {}
}
