package com.esign.validation.user;

import com.esign.annotation.user.PasswordMatches;
import com.esign.entities.user.ResetPasswordRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, ResetPasswordRequest> {
    @Override
    public boolean isValid(ResetPasswordRequest request, ConstraintValidatorContext context) {
        if (request.getPassword() == null || request.getConfirmPassword() == null) {
            return false;
        }

        boolean matches = request.getPassword().equals(request.getConfirmPassword());
        if (!matches) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Password and confirm password do not match")
                    .addPropertyNode("confirmPassword") // ← set path ke field confirmPassword
                    .addConstraintViolation();
        }

        return matches;
    }
}