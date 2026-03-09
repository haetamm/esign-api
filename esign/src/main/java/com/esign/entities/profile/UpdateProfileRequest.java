package com.esign.entities.profile;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class UpdateProfileRequest extends ProfileRequest {
    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "must contain only alphanumeric characters")
    @Size(min = 3, max = 8)
    private String username;

    @NotBlank
    @Email(
            message = "must be a valid email address",
            regexp = "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$"
    )
    private String email;
}
