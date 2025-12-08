package com.gdn.project.waroenk.cart.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Embedded document representing an item within a cart.
 * Stores snapshotted product data (name, price, stock, attributes).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {
    private String sku;
    private String subSku;           // Variant sub-SKU for inventory lookup
    private String title;            // Product/variant title snapshot
    private Long priceSnapshot;      // Price at time of adding to cart
    private Integer quantity;        // Quantity requested by user
    private Integer availableStockSnapshot;  // Stock available at time of last check
    private String imageUrl;
    private Map<String, String> attributes;  // Variant attributes snapshot
}





