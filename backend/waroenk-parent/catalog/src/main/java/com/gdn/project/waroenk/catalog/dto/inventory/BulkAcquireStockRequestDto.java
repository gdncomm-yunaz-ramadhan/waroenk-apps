package com.gdn.project.waroenk.catalog.dto.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Request DTO for bulk acquire stock operation - confirm reservation and reduce stock.
 */
public record BulkAcquireStockRequestDto(
    @NotBlank(message = "checkoutId is required")
    String checkoutId,
    @NotEmpty(message = "items list cannot be empty")
    @Valid
    List<StockOperationItemDto> items
) {}






