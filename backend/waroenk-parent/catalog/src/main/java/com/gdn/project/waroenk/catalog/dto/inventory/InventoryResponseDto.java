package com.gdn.project.waroenk.catalog.dto.inventory;

import java.time.Instant;

public record InventoryResponseDto(
    String id,
    String subSku,
    Long stock,
    Instant createdAt,
    Instant updatedAt
) {}









