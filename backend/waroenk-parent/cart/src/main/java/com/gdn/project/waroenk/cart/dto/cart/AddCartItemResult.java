package com.gdn.project.waroenk.cart.dto.cart;

import com.gdn.project.waroenk.cart.entity.Cart;

/**
 * Result of adding item to cart with stock information.
 */
public record AddCartItemResult(
    Cart cart,
    boolean success,
    String message,
    int availableStock
) {
    public static AddCartItemResult success(Cart cart, int availableStock) {
        return new AddCartItemResult(cart, true, "Item added to cart", availableStock);
    }
    
    public static AddCartItemResult insufficientStock(Cart cart, int availableStock) {
        return new AddCartItemResult(cart, false, 
                "Insufficient stock. Available: " + availableStock, availableStock);
    }
    
    public static AddCartItemResult outOfStock(Cart cart) {
        return new AddCartItemResult(cart, false, "Item is out of stock", 0);
    }
    
    public static AddCartItemResult productNotFound(Cart cart, String subSku) {
        return new AddCartItemResult(cart, false, 
                "Product not found for subSku: " + subSku, 0);
    }
}






