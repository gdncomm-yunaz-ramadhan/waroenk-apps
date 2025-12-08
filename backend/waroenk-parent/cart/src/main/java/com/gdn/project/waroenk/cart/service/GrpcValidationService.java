package com.gdn.project.waroenk.cart.service;

import com.gdn.project.waroenk.cart.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * Validation service for gRPC controllers.
 * <p>
 * Since gRPC uses protobuf-generated classes that cannot be annotated with
 * Jakarta Bean Validation annotations, this service provides validation
 * logic that can be called directly from gRPC controllers.
 */
@Service
@RequiredArgsConstructor
public class GrpcValidationService {

  private static final int MAX_CART_ITEM_QUANTITY = 999;
  private static final int MAX_BULK_ITEMS = 50;

  // ==================== Common Validations ====================

  /**
   * Validates that a required string field is not blank.
   *
   * @param value     the value to check
   * @param fieldName the field name for the error message
   * @throws ValidationException if the value is blank
   */
  public void validateRequired(String value, String fieldName) {
    if (StringUtils.isBlank(value)) {
      throw new ValidationException(fieldName + " is required");
    }
  }

  /**
   * Validates that a required object is not null.
   *
   * @param value     the value to check
   * @param fieldName the field name for the error message
   * @throws ValidationException if the value is null
   */
  public void validateRequiredObject(Object value, String fieldName) {
    if (value == null) {
      throw new ValidationException(fieldName + " is required");
    }
  }

  /**
   * Validates that a collection is not null or empty.
   *
   * @param collection the collection to check
   * @param fieldName  the field name for the error message
   * @throws ValidationException if the collection is null or empty
   */
  public void validateRequiredCollection(Collection<?> collection, String fieldName) {
    if (collection == null || collection.isEmpty()) {
      throw new ValidationException(fieldName + " is required and cannot be empty");
    }
  }

  /**
   * Validates an ID is not blank.
   *
   * @param id        the ID to validate
   * @param fieldName the field name for the error message
   * @throws ValidationException if the ID is blank
   */
  public void validateId(String id, String fieldName) {
    validateRequired(id, fieldName);
  }

  // ==================== Cart Validations ====================

  /**
   * Validates a user ID for cart operations.
   *
   * @param userId the user ID to validate
   * @throws ValidationException if the user ID is invalid
   */
  public void validateUserId(String userId) {
    validateRequired(userId, "User ID");
  }

  /**
   * Validates an add to cart request.
   *
   * @param userId   the user ID
   * @param sku      the product SKU
   * @param quantity the quantity to add
   * @throws ValidationException if validation fails
   */
  public void validateAddCartItem(String userId, String sku, Integer quantity) {
    validateUserId(userId);
    validateRequired(sku, "SKU");
    validateCartQuantity(quantity);
  }

  /**
   * Validates an add to cart request with sub-SKU.
   *
   * @param userId   the user ID
   * @param sku      the product SKU
   * @param subSku   the variant sub-SKU
   * @param quantity the quantity to add
   * @throws ValidationException if validation fails
   */
  public void validateAddCartItemWithSubSku(String userId, String sku, String subSku, Integer quantity) {
    validateUserId(userId);
    validateRequired(sku, "SKU");
    validateRequired(subSku, "Sub-SKU");
    validateCartQuantity(quantity);
  }

  /**
   * Validates a remove from cart request.
   *
   * @param userId the user ID
   * @param sku    the product SKU to remove
   * @throws ValidationException if validation fails
   */
  public void validateRemoveCartItem(String userId, String sku) {
    validateUserId(userId);
    validateRequired(sku, "SKU");
  }

  /**
   * Validates an update cart item request.
   *
   * @param userId   the user ID
   * @param sku      the product SKU
   * @param quantity the new quantity
   * @throws ValidationException if validation fails
   */
  public void validateUpdateCartItem(String userId, String sku, Integer quantity) {
    validateUserId(userId);
    validateRequired(sku, "SKU");
    validateCartQuantityAllowZero(quantity);
  }

  /**
   * Validates a bulk add items request.
   *
   * @param userId the user ID
   * @param items  the list of items to add
   * @throws ValidationException if validation fails
   */
  public void validateBulkAddCartItems(String userId, List<?> items) {
    validateUserId(userId);
    validateRequiredCollection(items, "Items");
    if (items.size() > MAX_BULK_ITEMS) {
      throw new ValidationException("Cannot add more than " + MAX_BULK_ITEMS + " items at once");
    }
  }

  /**
   * Validates a bulk remove items request.
   *
   * @param userId the user ID
   * @param skus   the list of SKUs to remove
   * @throws ValidationException if validation fails
   */
  public void validateBulkRemoveCartItems(String userId, List<String> skus) {
    validateUserId(userId);
    validateRequiredCollection(skus, "SKUs");
    if (skus.size() > MAX_BULK_ITEMS) {
      throw new ValidationException("Cannot remove more than " + MAX_BULK_ITEMS + " items at once");
    }
  }

  /**
   * Validates a clear cart request.
   *
   * @param userId the user ID
   * @throws ValidationException if validation fails
   */
  public void validateClearCart(String userId) {
    validateUserId(userId);
  }

  /**
   * Validates cart item quantity.
   *
   * @param quantity the quantity to validate
   * @throws ValidationException if the quantity is invalid
   */
  public void validateCartQuantity(Integer quantity) {
    if (quantity == null) {
      throw new ValidationException("Quantity is required");
    }
    if (quantity <= 0) {
      throw new ValidationException("Quantity must be greater than 0");
    }
    if (quantity > MAX_CART_ITEM_QUANTITY) {
      throw new ValidationException("Quantity cannot exceed " + MAX_CART_ITEM_QUANTITY);
    }
  }

  /**
   * Validates cart item quantity (allows zero for removal).
   *
   * @param quantity the quantity to validate
   * @throws ValidationException if the quantity is invalid
   */
  public void validateCartQuantityAllowZero(Integer quantity) {
    if (quantity == null) {
      throw new ValidationException("Quantity is required");
    }
    if (quantity < 0) {
      throw new ValidationException("Quantity cannot be negative");
    }
    if (quantity > MAX_CART_ITEM_QUANTITY) {
      throw new ValidationException("Quantity cannot exceed " + MAX_CART_ITEM_QUANTITY);
    }
  }

  // ==================== Checkout Validations ====================

  /**
   * Validates a checkout ID.
   *
   * @param checkoutId the checkout ID to validate
   * @throws ValidationException if the checkout ID is invalid
   */
  public void validateCheckoutId(String checkoutId) {
    validateRequired(checkoutId, "Checkout ID");
  }

  /**
   * Validates a prepare checkout request.
   *
   * @param userId the user ID
   * @throws ValidationException if validation fails
   */
  public void validatePrepareCheckout(String userId) {
    validateUserId(userId);
  }

  /**
   * Validates a finalize checkout request.
   *
   * @param checkoutId the checkout ID
   * @param addressId  the address ID (optional if newAddress provided)
   * @param hasNewAddress whether a new address is provided
   * @throws ValidationException if validation fails
   */
  public void validateFinalizeCheckout(String checkoutId, String addressId, boolean hasNewAddress) {
    validateCheckoutId(checkoutId);
    if (StringUtils.isBlank(addressId) && !hasNewAddress) {
      throw new ValidationException("Either address ID or new address is required");
    }
  }

  /**
   * Validates a pay checkout request.
   *
   * @param checkoutId the checkout ID
   * @throws ValidationException if validation fails
   */
  public void validatePayCheckout(String checkoutId) {
    validateCheckoutId(checkoutId);
  }

  /**
   * Validates a cancel checkout request.
   *
   * @param checkoutId the checkout ID
   * @throws ValidationException if validation fails
   */
  public void validateCancelCheckout(String checkoutId) {
    validateCheckoutId(checkoutId);
  }

  /**
   * Validates a get checkout by user request.
   *
   * @param userId the user ID
   * @throws ValidationException if validation fails
   */
  public void validateGetCheckoutByUser(String userId) {
    validateUserId(userId);
  }

  // ==================== Ownership Validations ====================

  /**
   * Validates that a checkout belongs to the authenticated user.
   *
   * @param checkoutId the checkout ID
   * @param userId     the authenticated user ID
   * @throws ValidationException if validation fails
   */
  public void validateCheckoutOwnership(String checkoutId, String userId) {
    validateCheckoutId(checkoutId);
    validateUserId(userId);
  }

  /**
   * Validates a finalize checkout request with ownership.
   *
   * @param checkoutId the checkout ID
   * @param userId     the authenticated user ID
   * @param addressId  the address ID (optional if newAddress provided)
   * @param hasNewAddress whether a new address is provided
   * @throws ValidationException if validation fails
   */
  public void validateFinalizeCheckoutWithOwnership(String checkoutId, String userId, 
                                                      String addressId, boolean hasNewAddress) {
    validateCheckoutOwnership(checkoutId, userId);
    if (StringUtils.isBlank(addressId) && !hasNewAddress) {
      throw new ValidationException("Either address ID or new address is required");
    }
  }

  /**
   * Validates a pay checkout request with ownership.
   *
   * @param checkoutId the checkout ID
   * @param userId     the authenticated user ID
   * @throws ValidationException if validation fails
   */
  public void validatePayCheckoutWithOwnership(String checkoutId, String userId) {
    validateCheckoutOwnership(checkoutId, userId);
  }

  /**
   * Validates a cancel checkout request with ownership.
   *
   * @param checkoutId the checkout ID
   * @param userId     the authenticated user ID
   * @throws ValidationException if validation fails
   */
  public void validateCancelCheckoutWithOwnership(String checkoutId, String userId) {
    validateCheckoutOwnership(checkoutId, userId);
  }

  /**
   * Validates a get checkout request with ownership.
   *
   * @param checkoutId the checkout ID
   * @param userId     the authenticated user ID
   * @throws ValidationException if validation fails
   */
  public void validateGetCheckoutWithOwnership(String checkoutId, String userId) {
    validateCheckoutOwnership(checkoutId, userId);
  }

  // ==================== Address Validations ====================

  /**
   * Validates a shipping address for checkout.
   *
   * @param recipientName the recipient name
   * @param phone         the phone number
   * @param addressLine   the address line
   * @param city          the city
   * @param province      the province
   * @param postalCode    the postal code
   * @throws ValidationException if validation fails
   */
  public void validateShippingAddress(String recipientName, String phone, String addressLine,
                                       String city, String province, String postalCode) {
    validateRequired(recipientName, "Recipient name");
    validateRequired(phone, "Phone number");
    validateRequired(addressLine, "Address");
    validateRequired(city, "City");
    validateRequired(province, "Province");
    validateRequired(postalCode, "Postal code");
  }
}

