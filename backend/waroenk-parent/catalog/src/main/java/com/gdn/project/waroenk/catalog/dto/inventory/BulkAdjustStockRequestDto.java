package com.gdn.project.waroenk.catalog.dto.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Request DTO for bulk adjust stock operation.
 */
public record BulkAdjustStockRequestDto(
    @NotEmpty(message = "items list cannot be empty")
    @Valid
    List<AdjustStockItemDto> items
) {
  public record AdjustStockItemDto(
      String subSku,
      Long quantity // Can be positive or negative
  ) {}
}







