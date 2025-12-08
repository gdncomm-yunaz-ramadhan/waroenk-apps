package com.gdn.project.waroenk.catalog.dto.inventory;

import java.time.Instant;

/**
 * Single inventory check item with stock and availability.
 */
public record InventoryCheckItemDto(
    String subSku,
    Long stock,
    boolean hasStock,
    Instant updatedAt
) {}







