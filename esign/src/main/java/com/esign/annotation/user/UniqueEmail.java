package com.esign.annotation.user;

import com.esign.constant.StatusMessage;
import com.esign.validation.user.UniqueEmailValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = UniqueEmailValidator.class)
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueEmail {
    String message() default StatusMessage.EMAIL_BEEN_TAKEN;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
