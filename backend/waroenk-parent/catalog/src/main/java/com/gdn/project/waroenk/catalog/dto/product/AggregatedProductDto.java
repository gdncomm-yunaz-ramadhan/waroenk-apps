package com.gdn.project.waroenk.catalog.dto.product;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record AggregatedProductDto(
    String id,
    String merchantName,
    String merchantCode,
    String merchantLocation,
    boolean inStock,
    String title,
    String summary,
    String brand,
    String category,
    String categoryCode,
    List<String> categoryNames,
    List<String> categoryCodes,
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
