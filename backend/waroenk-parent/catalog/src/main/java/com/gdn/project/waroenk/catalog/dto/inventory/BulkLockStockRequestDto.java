package com.gdn.project.waroenk.catalog.dto.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Request DTO for bulk lock stock operation - reserve inventory for checkout.
 */
public record BulkLockStockRequestDto(
    @NotBlank(message = "checkoutId is required")
    String checkoutId,
    @NotEmpty(message = "items list cannot be empty")
    @Valid
    List<StockOperationItemDto> items,
    Integer lockTtlSeconds // TTL for the lock, default 900 (15 min) if null
) {}







