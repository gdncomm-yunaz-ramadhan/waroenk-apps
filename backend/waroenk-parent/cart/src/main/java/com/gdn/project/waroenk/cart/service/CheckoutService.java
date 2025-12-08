package com.gdn.project.waroenk.cart.service;

import com.gdn.project.waroenk.cart.FilterCheckoutRequest;
import com.gdn.project.waroenk.cart.MultipleCheckoutResponse;
import com.gdn.project.waroenk.cart.dto.checkout.FinalizeCheckoutResult;
import com.gdn.project.waroenk.cart.dto.checkout.PayCheckoutResult;
import com.gdn.project.waroenk.cart.dto.checkout.PrepareCheckoutResult;
import com.gdn.project.waroenk.cart.entity.AddressSnapshot;
import com.gdn.project.waroenk.cart.entity.Checkout;
import com.gdn.project.waroenk.cart.exceptions.AuthorizationException;

/**
 * Service interface for checkout operations.
 * Follows the checkout lifecycle: WAITING → PAID | CANCELLED | EXPIRED
 */
public interface CheckoutService {

  // ============================================================
  // Main Checkout Flow
  // ============================================================

  /**
   * Prepare checkout - bulk lock inventory from cart items.
   * Creates checkout with WAITING status.
   *
   * @param userId User ID
   * @return PrepareCheckoutResult with checkout and lock summary
   */
  PrepareCheckoutResult prepareCheckout(String userId);

  /**
   * Finalize checkout - set shipping address and generate order ID / payment code.
   * Validates that the checkout belongs to the authenticated user.
   *
   * @param checkoutId Checkout ID
   * @param userId     Authenticated user ID for ownership validation
   * @param addressId  Optional existing address ID (from member service)
   * @param newAddress Optional new address to use
   * @return FinalizeCheckoutResult with order ID and payment code
   * @throws AuthorizationException if the checkout doesn't belong to the user
   */
  FinalizeCheckoutResult finalizeCheckout(String checkoutId, String userId, String addressId, AddressSnapshot newAddress);

  /**
   * Pay checkout - simulate payment and commit inventory permanently.
   * Validates that the checkout belongs to the authenticated user.
   *
   * @param checkoutId Checkout ID
   * @param userId     Authenticated user ID for ownership validation
   * @return PayCheckoutResult
   * @throws AuthorizationException if the checkout doesn't belong to the user
   */
  PayCheckoutResult payCheckout(String checkoutId, String userId);

  /**
   * Cancel checkout - release inventory lock.
   * Validates that the checkout belongs to the authenticated user.
   *
   * @param checkoutId Checkout ID
   * @param userId     Authenticated user ID for ownership validation
   * @param reason     Optional cancellation reason
   * @return true if cancelled successfully
   * @throws AuthorizationException if the checkout doesn't belong to the user
   */
  boolean cancelCheckout(String checkoutId, String userId, String reason);

  // ============================================================
  // Retrieval APIs
  // ============================================================

  /**
   * Get checkout by ID with ownership validation.
   *
   * @param checkoutId Checkout ID
   * @param userId     Authenticated user ID for ownership validation
   * @return Checkout entity
   * @throws AuthorizationException if the checkout doesn't belong to the user
   */
  Checkout getCheckout(String checkoutId, String userId);

  /**
   * Get active checkout by user ID (WAITING status)
   */
  Checkout getActiveCheckoutByUser(String userId);

  /**
   * List checkouts with cursor pagination
   */
  MultipleCheckoutResponse filterCheckouts(FilterCheckoutRequest request);
}