package com.esign.annotation.document;

import com.esign.validation.document.DocumentValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DocumentValidator.class)
@Documented
public @interface ValidDoc {
    String message() default "Invalid Document file";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    long maxSize() default 2 * 1024 * 1024; // 2MB default
}