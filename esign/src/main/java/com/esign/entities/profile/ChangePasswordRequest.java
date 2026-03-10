package com.esign.entities.profile;

import com.esign.annotation.user.PasswordMatches;
import com.esign.entities.user.ResetPasswordRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@PasswordMatches
public class ChangePasswordRequest extends ResetPasswordRequest {

    @NotBlank
    private String oldPassword;
}
