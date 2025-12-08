package com.gdn.project.waroenk.cart.dto.checkout;

import com.gdn.project.waroenk.cart.entity.Checkout;

import java.util.List;

/**
 * Result of prepare checkout operation with lock summary.
 */
public record PrepareCheckoutResult(
    Checkout checkout,
    boolean success,
    String message,
    List<SkuLockSummary> skuLockSummary
) {
    public record SkuLockSummary(
        String sku,
        String subSku,
        boolean locked,
        int requestedQuantity,
        int lockedQuantity,
        int availableStock,
        String errorMessage
    ) {
        public static SkuLockSummary success(String sku, String subSku, int quantity, int availableStock) {
            return new SkuLockSummary(sku, subSku, true, quantity, quantity, availableStock, null);
        }

        public static SkuLockSummary partialLock(String sku, String subSku, int requested, int locked, int available) {
            return new SkuLockSummary(sku, subSku, true, requested, locked, available,
                    "Partial lock: requested " + requested + ", locked " + locked);
        }

        public static SkuLockSummary failed(String sku, String subSku, int requested, int available, String error) {
            return new SkuLockSummary(sku, subSku, false, requested, 0, available, error);
        }
    }

    public static PrepareCheckoutResult success(Checkout checkout, List<SkuLockSummary> summary) {
        return new PrepareCheckoutResult(checkout, true, "Checkout prepared successfully", summary);
    }

    public static PrepareCheckoutResult partialSuccess(Checkout checkout, List<SkuLockSummary> summary) {
        return new PrepareCheckoutResult(checkout, true, 
                "Checkout prepared with some items unavailable", summary);
    }

    public static PrepareCheckoutResult emptyCart() {
        return new PrepareCheckoutResult(null, false, "Cart is empty", List.of());
    }

    public static PrepareCheckoutResult noItemsLocked(List<SkuLockSummary> summary) {
        return new PrepareCheckoutResult(null, false, "No items could be locked", summary);
    }

    public static PrepareCheckoutResult existingCheckout(Checkout checkout) {
        return new PrepareCheckoutResult(checkout, true, "Existing checkout found", List.of());
    }
}






