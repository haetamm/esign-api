package com.esign.annotation.role;

import com.esign.constant.StatusMessage;
import com.esign.validation.role.UniqueNameValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = UniqueNameValidator.class)
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueName {
    String message() default StatusMessage.ROLE_ALREADY_EXIST;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
