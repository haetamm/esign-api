package com.esign.annotation.user;

import com.esign.validation.user.PasswordMatchesValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordMatchesValidator.class)
@Documented
public @interface PasswordMatches {

    String message() default "Password and confirm password do not match";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
