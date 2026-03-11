package com.esign.entities.folder;

import com.esign.constant.FolderPermissionType;
import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FolderContributorResponse {
    private String id;
    private String userId;
    private String username;
    private FolderPermissionType permissionType;
}
