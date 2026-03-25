package com.esign.service;

import com.esign.entities.JwtClaims;
import com.esign.model.User;

public interface JwtService {
    String generateToken(User user);
    String generateRefreshToken(User user);
    boolean isTokenInvalid(String token);
    JwtClaims getClaimsByToken(String token);
    String parseToken(String bearerToken);
}
