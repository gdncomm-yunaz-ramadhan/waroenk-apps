package com.gdn.project.waroenk.cart.dto.checkout;

import com.gdn.project.waroenk.cart.entity.Checkout;

/**
 * Result of pay checkout operation.
 */
public record PayCheckoutResult(
    Checkout checkout,
    boolean success,
    String message
) {
    public static PayCheckoutResult success(Checkout checkout) {
        return new PayCheckoutResult(checkout, true, "Payment successful");
    }

    public static PayCheckoutResult invalidStatus(Checkout checkout) {
        return new PayCheckoutResult(checkout, false, 
                "Checkout is not in WAITING status. Current status: " + checkout.getEffectiveStatus());
    }

    public static PayCheckoutResult expired(Checkout checkout) {
        return new PayCheckoutResult(checkout, false, "Checkout has expired");
    }

    public static PayCheckoutResult notFinalized(Checkout checkout) {
        return new PayCheckoutResult(checkout, false, 
                "Checkout must be finalized before payment");
    }

    public static PayCheckoutResult inventoryAcquireFailed(Checkout checkout, String error) {
        return new PayCheckoutResult(checkout, false, 
                "Failed to acquire inventory: " + error);
    }

    public static PayCheckoutResult alreadyPaid(Checkout checkout) {
        return new PayCheckoutResult(checkout, false, "Checkout already paid");
    }
}






