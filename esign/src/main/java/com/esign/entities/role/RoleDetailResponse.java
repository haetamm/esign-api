package com.esign.entities.role;

import com.esign.entities.permission.PermissionResponse;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoleDetailResponse {
    private String id;
    private String name;
    private List<PermissionResponse> permissions;
    private String createdAt;
    private String updatedAt;
}