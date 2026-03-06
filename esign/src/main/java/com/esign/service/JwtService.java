package com.esign.service;

import com.esign.entities.JwtClaims;
import com.esign.model.User;

public interface JwtService {
    String generateToken(User user);
    boolean verifyJwtToken(String token);
    JwtClaims getClaimsByToken(String token);
}
