package com.esign.entities.role;

import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchRoleRequest {
    private String name;
    private Boolean isActive;

    private Integer page;
    private Integer size;
    private String sortBy;
    private String direction;
}