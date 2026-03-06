package com.esign.entities;

import lombok.*;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JwtClaims {
    private String userId;
    private List<String> roles;
}
