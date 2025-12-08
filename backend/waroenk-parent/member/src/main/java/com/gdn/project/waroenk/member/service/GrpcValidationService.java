package com.gdn.project.waroenk.member.service;

import com.gdn.project.waroenk.member.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Validation service for gRPC controllers.
 * <p>
 * Since gRPC uses protobuf-generated classes that cannot be annotated with
 * Jakarta Bean Validation annotations, this service provides the same validation
 * logic that can be called directly from gRPC controllers.
 * <p>
 * This ensures consistency between:
 * - gRPC controllers (calling this service directly)
 * - REST/HTTP controllers (using Bean Validation annotations that delegate to this service)
 */
@Service
@RequiredArgsConstructor
public class GrpcValidationService {

  private static final String DEFAULT_PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$";
  private static final String DEFAULT_PASSWORD_REQUIREMENTS = "Password must be at least 8 characters with uppercase, lowercase, number, and special character (@$!%*?&)";
  private static final String DEFAULT_MIN_PASSWORD_LENGTH = "8";

  private final SystemParameterService systemParameterService;

  // ==================== Password Validation ====================

  /**
   * Validates the password against configured rules.
   * Equivalent to @StrongPasswordRequired annotation.
   *
   * @param password the password to validate
   * @throws ValidationException if password doesn't meet requirements
   */
  public void validateStrongPassword(String password) {
    if (StringUtils.isBlank(password)) {
      throw new ValidationException("Password is required");
    }

    int minLength = Integer.parseInt(
        systemParameterService.getVariableData("MIN_PASSWORD_LENGTH", DEFAULT_MIN_PASSWORD_LENGTH));
    if (password.length() < minLength) {
      throw new ValidationException("Password must be at least " + minLength + " characters");
    }

    String pattern = systemParameterService.getVariableData("PASSWORD_PATTERN", DEFAULT_PASSWORD_PATTERN);
    if (!Pattern.matches(pattern, password)) {
      String requirements = systemParameterService.getVariableData("PASSWORD_REQUIREMENTS", DEFAULT_PASSWORD_REQUIREMENTS);
      throw new ValidationException(requirements);
    }
  }

  /**
   * Checks if the password is valid without throwing an exception.
   *
   * @param password the password to validate
   * @return true if valid, false otherwise
   */
  public boolean isValidPassword(String password) {
    try {
      validateStrongPassword(password);
      return true;
    } catch (ValidationException e) {
      return false;
    }
  }

  /**
   * Gets the current password requirements message.
   *
   * @return the requirements message
   */
  public String getPasswordRequirements() {
    return systemParameterService.getVariableData("PASSWORD_REQUIREMENTS", DEFAULT_PASSWORD_REQUIREMENTS);
  }

  // ==================== Contact Validation ====================

  /**
   * Validates that at least one contact (email or phone) is provided.
   * Equivalent to @AtLeastOneContact annotation.
   *
   * @param email the email address
   * @param phone the phone number
   * @throws ValidationException if neither email nor phone is provided
   */
  public void validateAtLeastOneContact(String email, String phone) {
    boolean hasEmail = StringUtils.isNotBlank(email);
    boolean hasPhone = StringUtils.isNotBlank(phone);

    if (!hasEmail && !hasPhone) {
      throw new ValidationException("At least one contact (email or phone) is required");
    }
  }

  /**
   * Checks if at least one contact is valid without throwing an exception.
   *
   * @param email the email address
   * @param phone the phone number
   * @return true if at least one contact is provided, false otherwise
   */
  public boolean hasAtLeastOneContact(String email, String phone) {
    return StringUtils.isNotBlank(email) || StringUtils.isNotBlank(phone);
  }

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
}




