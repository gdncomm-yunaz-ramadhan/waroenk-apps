package com.gdn.project.waroenk.catalog.dto.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Request DTO for bulk release stock operation - return reserved stock.
 */
public record BulkReleaseStockRequestDto(
    @NotBlank(message = "checkoutId is required")
    String checkoutId,
    @NotEmpty(message = "items list cannot be empty")
    @Valid
    List<StockOperationItemDto> items
) {}







