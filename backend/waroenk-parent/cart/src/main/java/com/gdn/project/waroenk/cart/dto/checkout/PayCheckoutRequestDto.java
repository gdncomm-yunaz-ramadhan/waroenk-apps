package com.gdn.project.waroenk.cart.dto.checkout;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to pay checkout (simulate payment).
 */
public record PayCheckoutRequestDto(
    @NotBlank(message = "Checkout ID is required") String checkoutId
) {}






