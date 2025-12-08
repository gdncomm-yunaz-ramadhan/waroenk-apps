package com.gdn.project.waroenk.member.validation;

import com.gdn.project.waroenk.member.exceptions.ValidationException;
import com.gdn.project.waroenk.member.service.GrpcValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Component;

/**
 * Bean Validation constraint validator for strong password requirements.
 * Delegates actual validation logic to {@link GrpcValidationService} for consistency.
 */
@Component
@RequiredArgsConstructor
public class StrongPasswordRequiredValidator implements ConstraintValidator<StrongPasswordRequired, Object> {

  private final GrpcValidationService validationService;
  private String passwordField;

  @Override
  public void initialize(StrongPasswordRequired annotation) {
    this.passwordField = annotation.passwordField();
  }

  @Override
  public boolean isValid(Object obj, ConstraintValidatorContext context) {
    if (obj == null) {
      return false;
    }

    BeanWrapper wrapper = new BeanWrapperImpl(obj);
    String password = (String) wrapper.getPropertyValue(passwordField);

    try {
      validationService.validateStrongPassword(password);
      return true;
    } catch (ValidationException e) {
      addViolation(context, e.getMessage());
      return false;
    }
  }

  private void addViolation(ConstraintValidatorContext context, String message) {
    context.disableDefaultConstraintViolation();
    context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
  }
}
