package com.gdn.project.waroenk.catalog.dto.inventory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateInventoryRequestDto(
    @NotBlank(message = "SubSKU is required") String subSku,
    @NotNull(message = "Stock is required") Long stock
) {}









