package com.esign.validation.user;

import com.esign.annotation.user.PassIfNotBlank;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class PassIfNotBlankValidator implements ConstraintValidator<PassIfNotBlank, String> {

    private static final Pattern PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {

        if (value != null && !value.trim().isEmpty()) {
            return PATTERN.matcher(value).matches() && value.length() >= 4 && value.length() <= 8;
        }

        return true;
    }
}
