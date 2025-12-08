package com.gdn.project.waroenk.cart.dto.checkout;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to cancel checkout.
 */
public record CancelCheckoutRequestDto(
    @NotBlank(message = "Checkout ID is required") String checkoutId,
    String reason  // Optional cancellation reason
) {}






