package com.esign.entities.folder;

import com.esign.constant.FolderPermissionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;


@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FolderContributorRequest {

    @NotBlank(message = "User id is required")
    private String userId;

    @NotNull(message = "Permission type is required")
    private FolderPermissionType permissionType;
}
