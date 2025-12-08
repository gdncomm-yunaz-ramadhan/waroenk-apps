package com.gdn.project.waroenk.cart.dto.checkout;

import com.gdn.project.waroenk.cart.entity.Checkout;

/**
 * Result of finalize checkout operation.
 */
public record FinalizeCheckoutResult(
    Checkout checkout,
    boolean success,
    String message,
    String orderId,
    String paymentCode
) {
    public static FinalizeCheckoutResult success(Checkout checkout, String orderId, String paymentCode) {
        return new FinalizeCheckoutResult(checkout, true, "Checkout finalized successfully", 
                orderId, paymentCode);
    }

    public static FinalizeCheckoutResult invalidStatus(Checkout checkout) {
        return new FinalizeCheckoutResult(checkout, false, 
                "Checkout is not in WAITING status", null, null);
    }

    public static FinalizeCheckoutResult expired(Checkout checkout) {
        return new FinalizeCheckoutResult(checkout, false, "Checkout has expired", null, null);
    }

    public static FinalizeCheckoutResult addressRequired() {
        return new FinalizeCheckoutResult(null, false, 
                "Address is required. Provide addressId or newAddress", null, null);
    }

    public static FinalizeCheckoutResult addressNotFound(String addressId) {
        return new FinalizeCheckoutResult(null, false, 
                "Address not found: " + addressId, null, null);
    }

    public static FinalizeCheckoutResult alreadyFinalized(Checkout checkout) {
        return new FinalizeCheckoutResult(checkout, false, 
                "Checkout already finalized", checkout.getOrderId(), checkout.getPaymentCode());
    }
}







