package com.esign.entities.role;

import lombok.*;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoleResponse {
    private String id;
    private String name;
    private List<PermissionResponse> permissions;
}