package com.esign.service.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.esign.constant.StatusMessage;
import com.esign.entities.JwtClaims;
import com.esign.model.User;
import com.esign.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtServiceImpl implements JwtService {

    @Value("${esign-api.jwt.secret}")
    private String JWT_SECRET;

    @Value("${esign-api.jwt.issuer}")
    private String JWT_ISSUER;

    @Value("${esign-api.jwt.expiration}")
    private long JWT_EXPIRATION;

    @Value("${esign-api.jwt.refresh-expiration}")
    private long JWT_REFRESH_EXPIRATION;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String generateToken(User user) {
        return buildToken(user, JWT_EXPIRATION);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String generateRefreshToken(User user) {
        return buildToken(user, JWT_REFRESH_EXPIRATION);
    }

    private String buildToken(User user, long expiration) {
        try {
            Algorithm algorithm = Algorithm.HMAC512(JWT_SECRET);
            return JWT.create()
                    .withSubject(user.getId())
                    .withIssuedAt(Instant.now())
                    .withExpiresAt(Instant.now().plusSeconds(expiration))
                    .withClaim("roles", user.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .toList())
                    .withIssuer(JWT_ISSUER)
                    .sign(algorithm);
        } catch (JWTCreationException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, StatusMessage.ERROR_CREATING_JWT);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean isTokenInvalid(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC512(JWT_SECRET);
            JWT.require(algorithm)
                    .withIssuer(JWT_ISSUER)
                    .build()
                    .verify(parseToken(token));
            return false;   // ← valid = tidak invalid
        } catch (JWTVerificationException e) {
            log.error("Invalid JWT Signature/Claims : {}", e.getMessage());
            return true;    // ← exception = invalid
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public JwtClaims getClaimsByToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC512(JWT_SECRET);
            DecodedJWT decodedJWT = JWT.require(algorithm)
                    .withIssuer(JWT_ISSUER)
                    .build()
                    .verify(parseToken(token));
            return JwtClaims.builder()
                    .userId(decodedJWT.getSubject())
                    .roles(decodedJWT.getClaim("roles").asList(String.class))
                    .build();
        } catch (JWTVerificationException e) {
            log.error("Invalid JWT Signature/Claims : {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, StatusMessage.UNAUTHORIZED);
        }
    }

    @Override
    public String parseToken(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return bearerToken;
    }
}
