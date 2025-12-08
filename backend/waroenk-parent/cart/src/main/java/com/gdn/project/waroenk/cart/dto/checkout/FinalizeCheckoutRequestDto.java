package com.gdn.project.waroenk.cart.dto.checkout;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to finalize checkout with shipping address.
 */
public record FinalizeCheckoutRequestDto(
    @NotBlank(message = "Checkout ID is required") String checkoutId,
    String addressId,           // Optional: use existing address from member service
    AddressInputDto newAddress  // Optional: provide new address inline
) {
    /**
     * New address input for checkout
     */
    public record AddressInputDto(
        @NotBlank(message = "Recipient name is required") String recipientName,
        @NotBlank(message = "Phone is required") String phone,
        @NotBlank(message = "Street is required") String street,
        @NotBlank(message = "City is required") String city,
        String province,
        String district,
        String subDistrict,
        @NotBlank(message = "Country is required") String country,
        @NotBlank(message = "Postal code is required") String postalCode,
        String notes,
        Float latitude,
        Float longitude
    ) {}
}





