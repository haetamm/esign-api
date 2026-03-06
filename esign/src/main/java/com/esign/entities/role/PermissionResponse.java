package com.esign.entities.role;

import com.esign.constant.ActionType;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PermissionResponse {
    private String id;
    private String url;
    private ActionType action;
}