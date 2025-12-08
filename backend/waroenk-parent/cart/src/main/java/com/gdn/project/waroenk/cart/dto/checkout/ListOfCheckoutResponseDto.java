package com.gdn.project.waroenk.cart.dto.checkout;

import java.util.List;

/**
 * List response for checkout filter operation.
 */
public record ListOfCheckoutResponseDto(
    List<CheckoutResponseDto> data,
    String nextToken,
    int total
) {}







