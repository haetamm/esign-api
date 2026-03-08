package com.esign.service.impl;

import com.esign.constant.StatusMessage;
import com.esign.entities.role.*;
import com.esign.exception.BadRequestException;
import com.esign.exception.NotFoundException;
import com.esign.helper.Utilities;
import com.esign.model.Permission;
import com.esign.model.Role;
import com.esign.model.RolePermission;
import com.esign.model.User;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    public RoleDetailResponse create(RoleRequest request) throws NotFoundException, BadRequestException {
        validationUtil.validate(request);

        boolean exists = roleRepository.existsByName(request.getName().toUpperCase());
        if (exists) throw new BadRequestException(StatusMessage.ROLE_ALREADY_EXIST);

        Role role = roleRepository.save(Role.builder()
                .name(request.getName().toUpperCase())
                .build());

        return getRoleResponse(request, role);
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
    public RoleDetailResponse update(String id, RoleRequest request) throws NotFoundException {
        validationUtil.validate(request);
        Role role = findByIdOrThrow(id);

        role.setName(request.getName().toUpperCase());
        roleRepository.save(role);

        rolePermissionRepository.deleteByRole(role);

        return getRoleResponse(request, role);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String toggleStatus(String id) throws NotFoundException {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(StatusMessage.ROLE_NOT_FOUND));
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

    private RoleResponse setRoleResponse(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .isActive(role.getIsActive())
                .build();
    }

    private RoleDetailResponse getRoleResponse(RoleRequest request, Role role) throws NotFoundException {
        List<Permission> permissions = permissionRepository.findAllByIdIn(request.getPermissionIds());
        if (permissions.isEmpty()) throw new NotFoundException(StatusMessage.PERMISSIONS_NOT_FOUND);

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