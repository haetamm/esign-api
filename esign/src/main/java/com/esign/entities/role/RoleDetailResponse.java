package com.esign.entities.role;

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