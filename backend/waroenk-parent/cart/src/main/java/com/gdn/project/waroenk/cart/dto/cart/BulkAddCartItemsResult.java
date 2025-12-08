package com.gdn.project.waroenk.cart.dto.cart;

import com.gdn.project.waroenk.cart.entity.Cart;

import java.util.List;

/**
 * Result of bulk adding items to cart with individual item status.
 */
public record BulkAddCartItemsResult(
    Cart cart,
    boolean allSuccess,
    List<CartItemStatus> itemStatuses
) {
    public static BulkAddCartItemsResult success(Cart cart, List<CartItemStatus> statuses) {
        boolean allSuccess = statuses.stream().allMatch(CartItemStatus::success);
        return new BulkAddCartItemsResult(cart, allSuccess, statuses);
    }
    
    public record CartItemStatus(
        String sku,
        String subSku,
        boolean success,
        String message,
        int availableStock
    ) {
        public static CartItemStatus success(String sku, String subSku, int availableStock) {
            return new CartItemStatus(sku, subSku, true, "Item added", availableStock);
        }
        
        public static CartItemStatus insufficientStock(String sku, String subSku, int availableStock, int requested) {
            return new CartItemStatus(sku, subSku, false, 
                    "Insufficient stock. Requested: " + requested + ", Available: " + availableStock, 
                    availableStock);
        }
        
        public static CartItemStatus productNotFound(String sku, String subSku) {
            return new CartItemStatus(sku, subSku, false, "Product not found", 0);
        }
    }
}







