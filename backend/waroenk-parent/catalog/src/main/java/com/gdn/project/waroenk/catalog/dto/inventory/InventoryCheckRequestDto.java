package com.gdn.project.waroenk.catalog.dto.inventory;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Request DTO for batch inventory check by subSkus.
 */
public record InventoryCheckRequestDto(
    @NotEmpty(message = "subSkus list cannot be empty")
    List<String> subSkus
) {}






