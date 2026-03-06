package com.esign.service.impl;

import com.esign.constant.ActionType;
import com.esign.entities.user.LoginRequest;
import com.esign.entities.user.LoginResponse;
import com.esign.model.*;
import com.esign.repository.*;
import com.esign.model.*;
import com.esign.repository.*;
import com.esign.service.AuthService;
import com.esign.service.JwtService;
import com.esign.validation.ValidationUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final ValidationUtil validationUtil;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleRepository userRoleRepository;


    @Value("${esign_api.super-admin.name}")
    private String superAdminName;

    @Value("${esign_api.super-admin.username}")
    private String superAdminUsername;

    @Value("${esign_api.super-admin.email}")
    private String superAdminEmail;

    @Value("${esign_api.super-admin.password}")
    private String superAdminPassword;

    @Transactional(rollbackFor = Exception.class)
    @PostConstruct
    public void init() {
        seedPermissions();
        seedRoles();
        seedSuperAdmin();
    }

    private void seedPermissions() {
        List<String> urls = List.of(
                "/api/user",
                "/api/auth",
                "/api/role",
                "/api/permission"
        );

        for (String url : urls) {
            for (ActionType action : ActionType.values()) {
                boolean exists = permissionRepository.existsByUrlAndAction(url, action);
                if (!exists) {
                    permissionRepository.save(
                            Permission.builder()
                                    .url(url)
                                    .action(action)
                                    .build()
                    );
                }
            }
        }
    }

    private void seedRoles() {
        boolean exists = roleRepository.existsByName("SUPER_ADMIN");
        if (exists) return;

        Role superAdminRole = roleRepository.save(
                Role.builder()
                        .name("SUPER_ADMIN")
                        .build()
        );

        // assign semua permission ke SUPER_ADMIN
        List<Permission> allPermissions = permissionRepository.findAll();
        for (Permission permission : allPermissions) {
            rolePermissionRepository.save(
                    RolePermission.builder()
                            .role(superAdminRole)
                            .permission(permission)
                            .build()
            );
        }
    }

    private void seedSuperAdmin() {
        boolean exists = userRepository.existsByUsername(superAdminUsername);
        if (exists) return;

        User superAdmin = userRepository.save(
                User.builder()
                        .username(superAdminUsername)
                        .email(superAdminEmail)
                        .password(passwordEncoder.encode(superAdminPassword))
                        .isEnable(true)
                        .build()
        );

        Role superAdminRole = roleRepository.findByName("SUPER_ADMIN")
                .orElseThrow(() -> new RuntimeException("Role SUPER_ADMIN not found"));

        userRoleRepository.save(
                UserRole.builder()
                        .user(superAdmin)
                        .role(superAdminRole)
                        .build()
        );
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public LoginResponse login(LoginRequest request) {
        validationUtil.validate(request);
        Authentication authentication = new UsernamePasswordAuthenticationToken(request.getUsername(),
                request.getPassword());

        Authentication authenticate = authenticationManager.authenticate(authentication);
        SecurityContextHolder.getContext().setAuthentication(authenticate);

        User user = (User) authenticate.getPrincipal();
        return getLoginResponse(user);
    }

    private LoginResponse getLoginResponse(User user) {
        String token = jwtService.generateToken(user);
        return LoginResponse.builder()
                .username(user.getUsername())
                .token(token)
                .build();
    }
}
