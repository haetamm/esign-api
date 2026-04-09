package com.esign.service.impl;

import com.esign.constant.ApiUrl;
import com.esign.constant.RoleName;
import com.esign.constant.StatusMessage;
import com.esign.entities.permission.PermissionResponse;
import com.esign.entities.role.*;
import com.esign.exception.BadRequestException;
import com.esign.exception.NotFoundException;
import com.esign.exception.ValidationCustomException;
import com.esign.helper.Utilities;
import com.esign.model.Permission;
import com.esign.model.Role;
import com.esign.model.RolePermission;
import com.esign.repository.PermissionRepository;
import com.esign.repository.RolePermissionRepository;
import com.esign.repository.RoleRepository;
import com.esign.service.RoleService;
import com.esign.specification.RoleSpecification;
import com.esign.validation.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final ValidationUtil validationUtil;
    private final RoleSpecification roleSpecification;
    private final Utilities utilities;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public RoleDetailResponse create(RoleRequest request) throws BadRequestException {
        validationUtil.validate(request);

        Role role = roleRepository.save(Role.builder()
                .name(request.getName().toUpperCase())
                .build());

        // merge dengan profile permission
        List<String> permissionIds = mergeWithDefaultPermissions(request.getPermissionIds());
        request.setPermissionIds(permissionIds);

        // validasi permission dari request dulu sebelum save
        List<Permission> permissions = validatePermissions(permissionIds);

        return saveRolePermissions(permissions, role);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<RoleResponse> getAll(SearchRoleRequest request) {
        Specification<Role> spec = roleSpecification.specification(request);

        Pageable pageable = utilities.buildPageable(
                request.getPage(),
                request.getSize(),
                request.getSortBy(),
                request.getDirection(),
                "createdAt"
        );

        return roleRepository.findAll(spec, pageable)
                .map(this::setRoleResponse);
    }

    @Transactional(readOnly = true)
    @Override
    public RoleDetailResponse getById(String id) throws NotFoundException {
        Role role = findByIdOrThrow(id);
        List<Permission> permissions = rolePermissionRepository.findByRole(role)
                .stream().map(RolePermission::getPermission).toList();
        return setDetailResponse(role, permissions);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public RoleDetailResponse update(String id, UpdateRoleRequest request) throws NotFoundException, ValidationCustomException {
        validationUtil.validate(request);
        Role role = findByIdOrThrow(id);
        isSuperAdmin(role);

        updateNameIfChange(request.getName().toUpperCase(), role);
        roleRepository.save(role);

        rolePermissionRepository.deleteByRole(role);

        // tambah profile permission ke request jika belum ada
        List<String> permissionIds = mergeWithDefaultPermissions(request.getPermissionIds());
        request.setPermissionIds(permissionIds);

        // validasi permission dari request dulu sebelum save
        List<Permission> permissions = validatePermissions(permissionIds);

        return saveRolePermissions(permissions, role);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String toggleStatus(String id) throws NotFoundException {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(StatusMessage.ROLE_NOT_FOUND));
        isSuperAdmin(role);
        role.setIsActive(!role.getIsActive());
        roleRepository.save(role);
        return role.getIsActive() ? StatusMessage.SUCCESS_ACTIVE : StatusMessage.SUCCESS_INACTIVE;
    }

    @Transactional(readOnly = true)
    @Override
    public Role getEntityById(String id) {
        return findByIdOrThrow(id);
    }

    private Role findByIdOrThrow(String id) {
        return roleRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new NotFoundException(StatusMessage.ROLE_NOT_FOUND));
    }

    private List<Permission> validatePermissions(List<String> permissionIds) {
        List<Permission> permissions = permissionRepository.findAllByIdIn(permissionIds);
        if (permissions.size() != permissionIds.size()) {
            List<String> foundIds = permissions.stream()
                    .map(Permission::getId)
                    .toList();

            List<String> missingIds = permissionIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();

            throw new NotFoundException(
                    StatusMessage.PERMISSIONS_NOT_FOUND + ": " + missingIds
            );
        }

        return permissions;
    }

    private void updateNameIfChange(String newName, Role role) throws ValidationCustomException {
        if (newName != null && !newName.isBlank() && !newName.equals(role.getName())) {
            if (roleRepository.existsByName(newName)) {
                throw new ValidationCustomException(StatusMessage.ROLE_ALREADY_EXIST, "name");
            }
            role.setName(newName.toUpperCase());
        }
    }

    private List<String> mergeWithDefaultPermissions(List<String> requestPermissionIds) {
        List<String> defaultPermissionIds = permissionRepository
                .findAllByUrlIn(List.of(
                        ApiUrl.API_URL + ApiUrl.API_PROFILE,
                        ApiUrl.API_URL + ApiUrl.API_NOTIFICATION,
                        ApiUrl.API_URL + ApiUrl.API_DASHBOARD
                ))
                .stream()
                .map(Permission::getId)
                .filter(id -> !requestPermissionIds.contains(id))
                .toList();

        List<String> merged = new ArrayList<>(requestPermissionIds);
        merged.addAll(defaultPermissionIds);
        return merged;
    }
    private void isSuperAdmin(Role role) {
        if (Objects.equals(role.getName(), RoleName.SUPER_ADMIN)) {
            throw new AccessDeniedException("Cannot modify SUPER_ADMIN role");
        }
    }

    private RoleResponse setRoleResponse(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .isActive(role.getIsActive())
                .build();
    }

    private RoleDetailResponse saveRolePermissions(List<Permission> permissions, Role role) throws NotFoundException {
        List<RolePermission> rolePermissions = permissions.stream()
                .map(permission -> RolePermission.builder()
                        .role(role)
                        .permission(permission)
                        .build())
                .toList();

        rolePermissionRepository.saveAll(rolePermissions);
        return setDetailResponse(role, permissions);
    }

    private RoleDetailResponse setDetailResponse(Role role, List<Permission> permissions) {
        return RoleDetailResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .createdAt(String.valueOf(role.getCreatedAt()))
                .updatedAt(String.valueOf(role.getUpdatedAt()))
                .permissions(permissions.stream()
                        .map(p -> PermissionResponse.builder()
                                .id(p.getId())
                                .url(p.getUrl())
                                .action(p.getAction())
                                .build())
                        .toList())
                .build();
    }
}