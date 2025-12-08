package com.gdn.project.waroenk.cart.dto.checkout;

import java.util.List;

/**
 * Response for prepare checkout operation.
 */
public record PrepareCheckoutResponseDto(
    CheckoutResponseDto checkout,
    boolean success,
    String message,
    List<SkuLockSummaryDto> skuLockSummary
) {
    public record SkuLockSummaryDto(
        String sku,
        String subSku,
        boolean locked,
        int requestedQuantity,
        int lockedQuantity,
        int availableStock,
        String errorMessage
    ) {}
}






