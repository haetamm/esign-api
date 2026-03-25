package com.esign.security;

import com.esign.entities.JwtClaims;
import com.esign.model.User;
import com.esign.repository.TokenRepository;
import com.esign.service.JwtService;
import com.esign.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserService userService;
    private final TokenRepository tokenRepository;
    private static final String AUTH_HEADER = "Authorization";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String bearerToken = request.getHeader(AUTH_HEADER);

            // 1. Cek header ada dan format benar
            if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            // 2. Verifikasi signature & expiry via JWT
            if (jwtService.isTokenInvalid(bearerToken)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 3. Cek token tidak di-revoke (logout)
            String rawToken = bearerToken.substring(7);
            boolean isRevoked = tokenRepository
                    .findByAccessTokenAndIsRevokedFalse(rawToken)
                    .isEmpty();

            if (isRevoked) {
                log.warn("Token has been revoked for request: {}", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            // 4. Set authentication ke SecurityContext
            JwtClaims jwtClaims = jwtService.getClaimsByToken(bearerToken);
            User user = userService.getByUserId(jwtClaims.getUserId());

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            user.getId(),
                            null,
                            user.getAuthorities()
                    );
            authentication.setDetails(new WebAuthenticationDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }
        filterChain.doFilter(request, response);
    }
}
