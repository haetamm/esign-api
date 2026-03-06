package com.esign.entities.user;

import com.esign.annotation.user.UniqueEmail;
import com.esign.annotation.user.UniqueUsername;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterRequest {
    @NotBlank
    @Pattern(regexp = "^[a-zA-Z ]+$", message = "must contain only alphabet characters and spaces")
    @Size(min = 4, max = 23)
    private String name;

    @NotBlank
    @UniqueUsername
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "must contain only alphanumeric characters")
    @Size(min = 3, max = 8)
    private String username;

    @NotBlank
    @UniqueEmail
    @Email(message = "must be a valid email address")
    private String email;

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "must contain only alphanumeric characters")
    @Size(min = 4, max = 8)
    private String password;
}
