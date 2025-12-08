package com.gdn.project.waroenk.cart.service;

import com.gdn.project.waroenk.cart.FilterCartRequest;
import com.gdn.project.waroenk.cart.MultipleCartResponse;
import com.gdn.project.waroenk.cart.dto.cart.AddCartItemResult;
import com.gdn.project.waroenk.cart.dto.cart.BulkAddCartItemsResult;
import com.gdn.project.waroenk.cart.entity.Cart;
import com.gdn.project.waroenk.cart.entity.CartItem;

import java.util.List;

/**
 * Service interface for cart operations.
 * Stock validation is performed via gRPC calls to catalog service.
 */
public interface CartService {

  /**
   * Get cart by user ID
   */
  Cart getCart(String userId);

  /**
   * Add single item to cart with stock validation via catalog gRPC.
   *
   * @param userId   User ID
   * @param sku      Product SKU
   * @param subSku   Variant sub-SKU (required for stock lookup)
   * @param quantity Quantity to add
   * @return Result with cart, success status, and available stock
   */
  AddCartItemResult addItemWithValidation(String userId, String sku, String subSku, int quantity);

  /**
   * Add multiple items to cart with stock validation via catalog gRPC.
   *
   * @param userId User ID
   * @param items  List of (sku, subSku, quantity) tuples
   * @return Result with cart and individual item statuses
   */
  BulkAddCartItemsResult bulkAddItemsWithValidation(String userId, List<CartItemInput> items);

  /**
   * Add single item to cart (legacy - no stock validation)
   *
   * @deprecated Use addItemWithValidation instead
   */
  @Deprecated
  Cart addItem(String userId, CartItem item);

  /**
   * Add multiple items to cart (legacy - no stock validation)
   *
   * @deprecated Use bulkAddItemsWithValidation instead
   */
  @Deprecated
  Cart bulkAddItems(String userId, List<CartItem> items);

  /**
   * Remove single item from cart
   */
  Cart removeItem(String userId, String sku);

  /**
   * Remove multiple items from cart (bulk)
   */
  Cart bulkRemoveItems(String userId, List<String> skus);

  /**
   * Update item quantity in cart with stock validation via catalog gRPC.
   *
   * @param userId   User ID
   * @param sku      Product SKU
   * @param subSku   Variant sub-SKU
   * @param quantity New quantity
   * @return Result with cart, success status, and available stock
   */
  AddCartItemResult updateItemQuantityWithValidation(String userId, String sku, String subSku, int quantity);

  /**
   * Update item quantity in cart (legacy - no stock validation)
   *
   * @deprecated Use updateItemQuantityWithValidation instead
   */
  @Deprecated
  Cart updateItemQuantity(String userId, String sku, Integer quantity);

  /**
   * Clear entire cart
   */
  boolean clearCart(String userId);

  /**
   * Filter carts (admin)
   */
  MultipleCartResponse filterCarts(FilterCartRequest request);


  /**
   * Input for bulk add items
   */
  record CartItemInput(String sku, String subSku, int quantity) {
  }
}