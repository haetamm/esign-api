package com.esign.repository;

import com.esign.model.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, String> {

    Optional<Token> findByAccessTokenAndIsRevokedFalse(String accessToken);

    Optional<Token> findByRefreshTokenAndIsRevokedFalse(String refreshToken);

    @Modifying
    @Query("UPDATE Token t SET t.isRevoked = true WHERE t.user.id = :userId AND t.isRevoked = false")
    void revokeAllActiveTokenByUserId(@Param("userId") String userId);
}
