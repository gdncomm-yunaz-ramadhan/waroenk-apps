package com.gdn.project.waroenk.member.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = StrongPasswordRequiredValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPasswordRequired {
  String message() default "Password does not met requirement";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  String passwordField() default "password";
}
