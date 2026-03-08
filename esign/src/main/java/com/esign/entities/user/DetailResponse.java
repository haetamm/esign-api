package com.esign.entities.user;

import lombok.*;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DetailResponse {
    private String name;
    private String phone;
    private String address;
    private String birthPlace;
    private String birthDate;
    private String religion;
    private String gender;
    private String username;
    private String email;
    private List<String> roles;
    private String createdAt;
    private String updatedAt;
}

