package com.gdn.project.waroenk.member.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = AtLeastOneContactValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AtLeastOneContact {
  String message() default "Either email or phone number is required";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};

  String emailField() default "email";
  String phoneField() default "phone";
}
