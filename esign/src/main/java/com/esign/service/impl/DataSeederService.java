package com.esign.service.impl;

import com.esign.constant.ActionType;
import com.esign.constant.ApiUrl;
import com.esign.constant.RoleName;
import com.esign.model.*;
import com.esign.repository.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DataSeederService {
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
                ApiUrl.API_URL + ApiUrl.API_ROLE,
                ApiUrl.API_URL + ApiUrl.API_USER,
                ApiUrl.API_URL + ApiUrl.API_PROFILE,
                ApiUrl.API_URL + ApiUrl.API_FOLDER
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
        // buat role jika belum ada
        Role superAdminRole = roleRepository.findByName(RoleName.SUPER_ADMIN)
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .name(RoleName.SUPER_ADMIN)
                                .build()
                ));

        List<Permission> allPermissions = permissionRepository.findAll();
        List<Permission> existingPermissions = rolePermissionRepository.findByRole(superAdminRole)
                .stream()
                .map(RolePermission::getPermission)
                .toList();

        List<Permission> newPermissions = allPermissions.stream()
                .filter(p -> !existingPermissions.contains(p))
                .toList();

        for (Permission permission : newPermissions) {
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

        Profile profile = profileRepository.save(Profile.builder()
                .name(superAdminName)
                .gender("Laki Laki")
                .build());

        User superAdmin = userRepository.save(
                User.builder()
                        .username(superAdminUsername)
                        .email(superAdminEmail)
                        .password(passwordEncoder.encode(superAdminPassword))
                        .profile(profile)
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
}
