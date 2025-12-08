package com.gdn.project.waroenk.cart.dto.checkout;

import java.util.Map;

/**
 * Checkout item DTO representing locked cart items.
 */
public record CheckoutItemDto(
    String sku,
    String subSku,
    String title,
    Long priceSnapshot,
    Integer quantity,
    Integer availableStockSnapshot,
    String imageUrl,
    Map<String, String> attributes,
    Boolean reserved,
    String reservationError
) {}





