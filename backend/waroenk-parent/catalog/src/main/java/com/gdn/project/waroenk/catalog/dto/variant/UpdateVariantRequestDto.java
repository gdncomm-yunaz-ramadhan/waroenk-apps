package com.gdn.project.waroenk.catalog.dto.variant;

import java.util.List;
import java.util.Map;

public record UpdateVariantRequestDto(
    String sku,
    String title,
    Double price,
    Boolean isDefault,
    Map<String, Object> attributes,
    String thumbnail,
    List<VariantMediaDto> media
) {
  public record VariantMediaDto(
      String url,
      String type,
      Integer sortOrder,
      String altText
  ) {}
}
