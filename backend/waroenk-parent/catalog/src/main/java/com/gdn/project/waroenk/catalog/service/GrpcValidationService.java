package com.gdn.project.waroenk.catalog.service;

import com.gdn.project.waroenk.catalog.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collection;

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
   * Validates that a string field has a minimum length.
   *
   * @param value     the value to check
   * @param minLength the minimum length
   * @param fieldName the field name for the error message
   * @throws ValidationException if the value is shorter than minLength
   */
  public void validateMinLength(String value, int minLength, String fieldName) {
    if (StringUtils.isNotBlank(value) && value.trim().length() < minLength) {
      throw new ValidationException(fieldName + " must be at least " + minLength + " characters");
    }
  }

  /**
   * Validates that a string field has a maximum length.
   *
   * @param value     the value to check
   * @param maxLength the maximum length
   * @param fieldName the field name for the error message
   * @throws ValidationException if the value exceeds maxLength
   */
  public void validateMaxLength(String value, int maxLength, String fieldName) {
    if (StringUtils.isNotBlank(value) && value.length() > maxLength) {
      throw new ValidationException(fieldName + " must not exceed " + maxLength + " characters");
    }
  }

  // ==================== ID Validations ====================

  /**
   * Validates a MongoDB ObjectId format (24 hex characters).
   *
   * @param id        the ID to validate
   * @param fieldName the field name for the error message
   * @throws ValidationException if the ID is invalid
   */
  public void validateMongoId(String id, String fieldName) {
    validateRequired(id, fieldName);
    if (!id.matches("^[a-fA-F0-9]{24}$")) {
      throw new ValidationException(fieldName + " is not a valid ID format");
    }
  }

  /**
   * Validates an ID is not blank (lenient - doesn't check format).
   *
   * @param id        the ID to validate
   * @param fieldName the field name for the error message
   * @throws ValidationException if the ID is blank
   */
  public void validateId(String id, String fieldName) {
    validateRequired(id, fieldName);
  }

  // ==================== Product Validations ====================

  /**
   * Validates a create product request.
   *
   * @param title        the product title
   * @param sku          the product SKU
   * @param merchantCode the merchant code
   * @throws ValidationException if validation fails
   */
  public void validateCreateProduct(String title, String sku, String merchantCode) {
    validateRequired(title, "Product title");
    validateRequired(sku, "SKU");
    validateRequired(merchantCode, "Merchant code");
  }

  /**
   * Validates an update product request.
   *
   * @param id           the product ID
   * @param title        the product title
   * @param sku          the product SKU
   * @param merchantCode the merchant code
   * @throws ValidationException if validation fails
   */
  public void validateUpdateProduct(String id, String title, String sku, String merchantCode) {
    validateId(id, "Product ID");
    validateRequired(title, "Product title");
    validateRequired(sku, "SKU");
    validateRequired(merchantCode, "Merchant code");
  }

  /**
   * Validates a SKU format.
   *
   * @param sku the SKU to validate
   * @throws ValidationException if the SKU is invalid
   */
  public void validateSku(String sku) {
    validateRequired(sku, "SKU");
    validateMinLength(sku, 3, "SKU");
    validateMaxLength(sku, 50, "SKU");
  }

  // ==================== Variant Validations ====================

  /**
   * Validates a create variant request.
   * Note: subSku is auto-generated by the service, so it's not validated here.
   *
   * @param sku   the parent product SKU
   * @param title the variant title
   * @param price the variant price
   * @throws ValidationException if validation fails
   */
  public void validateCreateVariant(String sku, String title, Double price) {
    validateRequired(sku, "SKU");
    validateRequired(title, "Variant title");
    validatePositivePrice(price, "Price");
  }

  /**
   * Validates an update variant request.
   *
   * @param id    the variant ID
   * @param sku   the parent product SKU
   * @param title the variant title
   * @param price the variant price
   * @throws ValidationException if validation fails
   */
  public void validateUpdateVariant(String id, String sku, String title, Double price) {
    validateId(id, "Variant ID");
    validateRequired(sku, "SKU");
    validateRequired(title, "Variant title");
    validatePositivePrice(price, "Price");
  }

  /**
   * Validates a sub-SKU.
   *
   * @param subSku the sub-SKU to validate
   * @throws ValidationException if the sub-SKU is invalid
   */
  public void validateSubSku(String subSku) {
    validateRequired(subSku, "Sub-SKU");
    validateMinLength(subSku, 3, "Sub-SKU");
    validateMaxLength(subSku, 100, "Sub-SKU");
  }

  // ==================== Merchant Validations ====================

  /**
   * Validates a create merchant request.
   *
   * @param name the merchant name
   * @param code the merchant code
   * @throws ValidationException if validation fails
   */
  public void validateCreateMerchant(String name, String code) {
    validateRequired(name, "Merchant name");
    validateRequired(code, "Merchant code");
    validateMaxLength(code, 20, "Merchant code");
  }

  /**
   * Validates an update merchant request.
   *
   * @param id   the merchant ID
   * @param name the merchant name
   * @param code the merchant code
   * @throws ValidationException if validation fails
   */
  public void validateUpdateMerchant(String id, String name, String code) {
    validateId(id, "Merchant ID");
    validateRequired(name, "Merchant name");
    validateRequired(code, "Merchant code");
  }

  // ==================== Category/Brand Validations ====================

  /**
   * Validates a create category/brand request.
   *
   * @param name       the category/brand name
   * @param entityType "Category" or "Brand"
   * @throws ValidationException if validation fails
   */
  public void validateCreateCategoryOrBrand(String name, String entityType) {
    validateRequired(name, entityType + " name");
  }

  /**
   * Validates an update category/brand request.
   *
   * @param id         the entity ID
   * @param name       the category/brand name
   * @param entityType "Category" or "Brand"
   * @throws ValidationException if validation fails
   */
  public void validateUpdateCategoryOrBrand(String id, String name, String entityType) {
    validateId(id, entityType + " ID");
    validateRequired(name, entityType + " name");
  }

  // ==================== Inventory Validations ====================

  /**
   * Validates inventory quantity.
   *
   * @param quantity the quantity to validate
   * @throws ValidationException if the quantity is invalid
   */
  public void validateInventoryQuantity(Integer quantity) {
    if (quantity == null) {
      throw new ValidationException("Quantity is required");
    }
    if (quantity < 0) {
      throw new ValidationException("Quantity cannot be negative");
    }
  }

  /**
   * Validates an update inventory request.
   *
   * @param subSku   the variant sub-SKU
   * @param quantity the quantity
   * @throws ValidationException if validation fails
   */
  public void validateUpdateInventory(String subSku, Integer quantity) {
    validateRequired(subSku, "Sub-SKU");
    validateInventoryQuantity(quantity);
  }

  // ==================== Price Validations ====================

  /**
   * Validates that a price is positive.
   *
   * @param price     the price to validate
   * @param fieldName the field name for the error message
   * @throws ValidationException if the price is invalid
   */
  public void validatePositivePrice(Double price, String fieldName) {
    if (price == null) {
      throw new ValidationException(fieldName + " is required");
    }
    if (price < 0) {
      throw new ValidationException(fieldName + " cannot be negative");
    }
  }

  // ==================== Search Validations ====================

  /**
   * Validates a search query.
   *
   * @param query     the search query
   * @param minLength the minimum length required
   * @throws ValidationException if the query is too short
   */
  public void validateSearchQuery(String query, int minLength) {
    if (StringUtils.isNotBlank(query) && query.trim().length() < minLength) {
      throw new ValidationException("Search query must be at least " + minLength + " characters");
    }
  }
}

