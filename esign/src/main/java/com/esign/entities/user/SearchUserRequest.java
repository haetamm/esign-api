package com.esign.entities.user;

import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchUserRequest {
    private String name;
    private String phone;
    private String gender;
    private String email;

    private Integer page;
    private Integer size;
    private String sortBy;
    private String direction;
}