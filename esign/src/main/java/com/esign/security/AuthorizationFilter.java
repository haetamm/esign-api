package com.esign.security;

import com.esign.constant.ActionType;
import com.esign.constant.StatusMessage;
import com.esign.entities.WebErrorResponse;
import com.esign.helper.Utilities;
import com.esign.repository.RolePermissionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthorizationFilter extends OncePerRequestFilter {

    private final RolePermissionRepository rolePermissionRepository;
    private final Utilities utilities;

    private static final List<String> PUBLIC_URLS = List.of(
            "/api/auth/login",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api-docs",
            "/swagger-ui"
    );

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String requestUrl = request.getRequestURI();
        String httpMethod = request.getMethod();

        // skip public urls
        boolean isPublic = PUBLIC_URLS.stream().anyMatch(requestUrl::startsWith);
        if (isPublic) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            utilities.writeError(response, HttpStatus.UNAUTHORIZED, StatusMessage.UNAUTHORIZED);
            return;
        }

        // ambil semua role user dari token
        List<String> userRoles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        // convert HTTP method ke ActionType
        ActionType requiredAction = mapHttpMethodToAction(httpMethod, requestUrl);

        // cek apakah role user punya permission untuk url + action ini
        boolean hasPermission = rolePermissionRepository
                .existsByRoleNameInAndPermissionUrlAndPermissionAction(
                        userRoles,
                        extractBaseUrl(requestUrl),
                        requiredAction
                );

        if (!hasPermission) {
            utilities.writeError(response, HttpStatus.FORBIDDEN, StatusMessage.ACCESS_DENIED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private ActionType mapHttpMethodToAction(String method, String url) {
        if (url.contains("/approve")) {
            return ActionType.APPROVE;
        }

        return switch (method.toUpperCase()) {
            case "POST"   -> ActionType.CREATE;
            case "PUT",
                 "PATCH"  -> ActionType.UPDATE;
            case "DELETE" -> ActionType.DELETE;
            default       -> ActionType.READ;
        };
    }

    // "/api/user/123" → "/api/user"
    private String extractBaseUrl(String url) {
        String[] parts = url.split("/");
        if (parts.length >= 3) {
            return "/" + parts[1] + "/" + parts[2];
        }
        return url;
    }
}
