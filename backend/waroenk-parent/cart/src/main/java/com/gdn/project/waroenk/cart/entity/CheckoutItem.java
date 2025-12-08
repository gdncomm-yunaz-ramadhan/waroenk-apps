package com.gdn.project.waroenk.cart.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Embedded document representing an item within a checkout.
 * Contains the final locked state of cart items.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutItem {
    private String sku;
    private String subSku;           // Variant sub-SKU used for inventory
    private String title;
    private Long priceSnapshot;
    private Integer quantity;        // Quantity locked for checkout
    private Integer availableStockSnapshot;  // Stock at time of lock
    private String imageUrl;
    private Map<String, String> attributes;
    
    @Builder.Default
    private Boolean reserved = false;  // Whether inventory is successfully locked
    
    private String reservationError;   // Error message if lock failed
}





