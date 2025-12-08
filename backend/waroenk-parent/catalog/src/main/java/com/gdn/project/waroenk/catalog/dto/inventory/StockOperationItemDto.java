package com.gdn.project.waroenk.catalog.dto.inventory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Single stock operation item for bulk lock/acquire/release operations.
 */
public record StockOperationItemDto(
    @NotBlank(message = "subSku is required")
    String subSku,
    @Positive(message = "quantity must be positive")
    Long quantity
) {}






