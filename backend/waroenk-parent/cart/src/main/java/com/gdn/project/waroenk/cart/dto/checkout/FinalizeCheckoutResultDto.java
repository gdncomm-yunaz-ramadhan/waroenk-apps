package com.gdn.project.waroenk.cart.dto.checkout;

/**
 * Result DTO for finalize checkout operation.
 */
public record FinalizeCheckoutResultDto(
    CheckoutResponseDto checkout,
    boolean success,
    String message,
    String orderId,
    String paymentCode
) {}






