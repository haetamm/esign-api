package com.esign.entities.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoleRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Permission ids is required")
    private List<String> permissionIds;
}
