package com.esign.annotation.user;


import com.esign.validation.user.PassIfNotBlankValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PassIfNotBlankValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PassIfNotBlank {

    String message() default "Password must contain only alphanumeric characters and size must be between 4 and 8";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
