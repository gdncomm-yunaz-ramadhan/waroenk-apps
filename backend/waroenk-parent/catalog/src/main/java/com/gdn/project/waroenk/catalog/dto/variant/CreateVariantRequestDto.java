package com.gdn.project.waroenk.catalog.dto.variant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record CreateVariantRequestDto(
    @NotBlank(message = "SKU is required") String sku,
    @NotBlank(message = "Title is required") String title,
    @NotNull(message = "Price is required") Double price,
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
