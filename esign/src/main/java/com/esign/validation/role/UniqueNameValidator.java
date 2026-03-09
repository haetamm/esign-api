package com.esign.validation.role;

import com.esign.annotation.role.UniqueName;
import com.esign.repository.RoleRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;

public class UniqueNameValidator implements ConstraintValidator<UniqueName, String> {
    @Autowired
    private RoleRepository roleRepository;

    @Override
    public boolean isValid(String name, ConstraintValidatorContext context) {
        return name != null && !roleRepository.existsByName(name.toUpperCase());
    }
}

