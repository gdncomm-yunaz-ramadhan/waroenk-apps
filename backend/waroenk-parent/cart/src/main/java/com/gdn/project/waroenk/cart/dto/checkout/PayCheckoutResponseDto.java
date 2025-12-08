package com.gdn.project.waroenk.cart.dto.checkout;

/**
 * Response for pay checkout operation.
 */
public record PayCheckoutResponseDto(
    CheckoutResponseDto checkout,
    boolean success,
    String message
) {}







