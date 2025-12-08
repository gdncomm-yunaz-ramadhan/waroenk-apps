package com.gdn.project.waroenk.cart.dto.checkout;

import java.time.Instant;
import java.util.List;

/**
 * Checkout response DTO with full checkout data.
 */
public record CheckoutResponseDto(
    String checkoutId,
    String userId,
    String orderId,
    String paymentCode,
    String sourceCartId,
    List<CheckoutItemDto> items,
    Long totalPrice,
    String currency,
    String status,
    AddressSnapshotDto shippingAddress,
    Instant lockedAt,
    Instant expiresAt,
    Instant createdAt,
    Instant paidAt,
    Instant cancelledAt
) {
    /**
     * Address snapshot DTO
     */
    public record AddressSnapshotDto(
        String recipientName,
        String phone,
        String street,
        String city,
        String province,
        String district,
        String subDistrict,
        String country,
        String postalCode,
        String notes,
        Float latitude,
        Float longitude
    ) {}
}





