package com.gdn.project.waroenk.cart.dto.checkout;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to prepare checkout (bulk lock inventory).
 */
public record PrepareCheckoutRequestDto(
    @NotBlank(message = "User ID is required") String userId
) {}






