package com.esign.entities.folder;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RenameRequest {
    @NotBlank(message = "Name is required")
    private String name;
}