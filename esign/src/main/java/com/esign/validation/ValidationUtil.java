package com.esign.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ValidationUtil {
    private final Validator validator;

    public void validate(Object object) {
        Set<ConstraintViolation<Object>> results = validator.validate(object);
        if (!results.isEmpty()) {
            throw new ConstraintViolationException(results);
        }
    }
}

