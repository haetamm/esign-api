package com.esign.entities.folder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FolderRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String parentId; // null = root folder

    @NotNull(message = "isRoleRestricted is required")
    private Boolean isRoleRestricted; // false = folder umum

    @NotNull(message = "isPublic is required")
    private Boolean isPublic;
}
