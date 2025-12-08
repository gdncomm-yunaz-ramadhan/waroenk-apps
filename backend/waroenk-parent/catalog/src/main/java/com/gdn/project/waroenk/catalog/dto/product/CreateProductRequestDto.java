package com.gdn.project.waroenk.catalog.dto.product;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateProductRequestDto(
    @NotBlank(message = "Title is required") String title,
    @NotBlank(message = "SKU is required") String sku,
    @NotBlank(message = "Merchant code is required") String merchantCode,
    @NotBlank(message = "Category ID is required") String categoryId,
    @NotBlank(message = "Brand ID is required") String brandId,
    ProductSummaryDto summary,
    String detailRef
) {
  public record ProductSummaryDto(
      String shortDescription,
      List<String> tags
  ) {}
}
