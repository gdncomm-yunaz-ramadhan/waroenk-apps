package com.gdn.project.waroenk.catalog.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record AggregatedProductDto(
    String id,
    String merchantName,
    String merchantLocation,
    boolean inStock,
    String title,
    String summary,
    String brand,
    String category,
    String thumbnail,
    String slug,
    Map<String, Object> attributes,
    List<String> variantKeywords,
    String sku,
    String subSku,
    Double price,
    Instant createdAt,
    Instant updatedAt
) {
}
