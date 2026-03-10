package com.esign.validation.profile;


import com.esign.annotation.profile.ValidImageFile;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public class ImageFileValidator implements ConstraintValidator<ValidImageFile, MultipartFile> {

    private static final List<String> ALLOWED_TYPES = List.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );

    private long maxSize;

    @Override
    public void initialize(ValidImageFile constraintAnnotation) {
        this.maxSize = constraintAnnotation.maxSize();
    }

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null || file.isEmpty()) {
            buildViolation(context, "Image file is required");
            return false;
        }

        // validasi tipe file
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            buildViolation(context, "Invalid image type. Allowed: jpg, jpeg, png, webp");
            return false;
        }

        // validasi ukuran file (max 2MB)
        if (file.getSize() > maxSize) {
            buildViolation(context, "Image size must not exceed " + (maxSize / (1024 * 1024)) + "MB");
            return false;
        }

        return true;
    }

    private void buildViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
