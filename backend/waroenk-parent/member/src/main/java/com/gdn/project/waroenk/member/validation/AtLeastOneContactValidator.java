package com.gdn.project.waroenk.member.validation;

import com.gdn.project.waroenk.member.service.GrpcValidationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Component;

/**
 * Bean Validation constraint validator for requiring at least one contact method.
 * Delegates actual validation logic to {@link GrpcValidationService} for consistency.
 */
@Component
@RequiredArgsConstructor
public class AtLeastOneContactValidator implements ConstraintValidator<AtLeastOneContact, Object> {

  private final GrpcValidationService validationService;
  private String emailField;
  private String phoneField;

  @Override
  public void initialize(AtLeastOneContact annotation) {
    this.emailField = annotation.emailField();
    this.phoneField = annotation.phoneField();
  }

  @Override
  public boolean isValid(Object obj, ConstraintValidatorContext context) {
    if (obj == null) {
      return false;
    }

    BeanWrapper wrapper = new BeanWrapperImpl(obj);
    String email = (String) wrapper.getPropertyValue(emailField);
    String phone = (String) wrapper.getPropertyValue(phoneField);

    return validationService.hasAtLeastOneContact(email, phone);
  }
}
