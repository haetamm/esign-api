package com.esign.validation.document;

import com.esign.annotation.document.ValidDoc;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

public class DocumentValidator implements ConstraintValidator<ValidDoc, MultipartFile> {

    private static final String ALLOWED_TYPE = "application/pdf";

    private long maxSize;

    @Override
    public void initialize(ValidDoc constraintAnnotation) {
        this.maxSize = constraintAnnotation.maxSize();
    }

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {

        // wajib ada file
        if (file == null || file.isEmpty()) {
            buildViolation(context, "PDF file is required");
            return false;
        }

        // validasi extension
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".pdf")) {
            buildViolation(context, "File must be a PDF");
            return false;
        }

        // validasi MIME type
        if (!ALLOWED_TYPE.equalsIgnoreCase(file.getContentType())) {
            buildViolation(context, "Invalid file type. Only PDF is allowed");
            return false;
        }

        // validasi ukuran (max 2MB)
        if (file.getSize() > maxSize) {
            buildViolation(context, "File size must not exceed 2MB");
            return false;
        }

        return true;
    }

    private void buildViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addConstraintViolation();
    }
}