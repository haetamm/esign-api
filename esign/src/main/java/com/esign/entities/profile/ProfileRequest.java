package com.esign.entities.profile;

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
public class ProfileRequest {
    @NotBlank
    @Pattern(regexp = "^[a-zA-Z ]+$", message = "must contain only alphabet characters and spaces")
    @Size(min = 4, max = 50)
    private String name;

    @Size(max = 20)
    private String phone;

    @Size(max = 225)
    private String address;

    @Size(max = 40)
    private String birthPlace;

    @Pattern(
            regexp = "^\\d{4}-\\d{2}-\\d{2}$",
            message = "Birth date must be in format yyyy-MM-dd"
    )
    private String birthDate;

    @Size(max = 10)
    private String religion;

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z ]+$", message = "must contain only alphabet characters and spaces")
    @Size(max = 10)
    private String gender;
}
