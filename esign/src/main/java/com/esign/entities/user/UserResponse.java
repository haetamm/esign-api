package com.esign.entities.user;

import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponse {
    private String id;
    private String name;
    private String phone;
    private String address;
    private String gender;
    private String email;
}

