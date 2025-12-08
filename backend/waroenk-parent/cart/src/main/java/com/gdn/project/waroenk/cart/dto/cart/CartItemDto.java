package com.gdn.project.waroenk.cart.dto.cart;

import java.util.Map;

/**
 * Cart item DTO with snapshot data from catalog.
 */
public record CartItemDto(
    String sku,
    String subSku,
    String title,
    Long priceSnapshot,
    Integer quantity,
    Integer availableStockSnapshot,
    String imageUrl,
    Map<String, String> attributes
) {}





