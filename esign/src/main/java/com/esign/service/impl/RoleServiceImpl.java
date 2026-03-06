package com.esign.service.impl;

import com.esign.constant.StatusMessage;
import com.esign.entities.role.PermissionResponse;
import com.esign.entities.role.RoleRequest;
import com.esign.entities.role.RoleResponse;
import com.esign.exception.BadRequestException;
import com.esign.exception.NotFoundException;
import com.esign.model.Permission;
import com.esign.model.Role;
import com.esign.model.RolePermission;
import com.esign.repository.PermissionRepository;
import com.esign.repository.RolePermissionRepository;
import com.esign.repository.RoleRepository;
import com.esign.service.RoleService;
import com.esign.validation.ValidationUtil;
import lombok.RequiredArgsConstructor;
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

    @Transactional(rollbackFor = Exception.class)
    @Override
    public RoleResponse create(RoleRequest request) throws NotFoundException, BadRequestException {
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
    public List<RoleResponse> getAll() {
        return roleRepository.findAllByIsActiveTrue().stream()
                .map(role -> {
                    List<Permission> permissions = rolePermissionRepository.findByRole(role)
                            .stream().map(RolePermission::getPermission).toList();
                    return toResponse(role, permissions);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public RoleResponse getById(String id) throws NotFoundException {
        Role role = findByIdOrThrow(id);
        List<Permission> permissions = rolePermissionRepository.findByRole(role)
                .stream().map(RolePermission::getPermission).toList();
        return toResponse(role, permissions);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public RoleResponse update(String id, RoleRequest request) throws NotFoundException {
        validationUtil.validate(request);
        Role role = findByIdOrThrow(id);

        role.setName(request.getName().toUpperCase());
        roleRepository.save(role);

        rolePermissionRepository.deleteByRole(role);

        return getRoleResponse(request, role);
    }

    private RoleResponse getRoleResponse(RoleRequest request, Role role) throws NotFoundException {
        List<Permission> permissions = permissionRepository.findAllByIdIn(request.getPermissionIds());
        if (permissions.isEmpty()) throw new NotFoundException(StatusMessage.PERMISSIONS_NOT_FOUND);

        List<RolePermission> rolePermissions = permissions.stream()
                .map(permission -> RolePermission.builder()
                        .role(role)
                        .permission(permission)
                        .build())
                .toList();

        rolePermissionRepository.saveAll(rolePermissions);
        return toResponse(role, permissions);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(String id) {
        Role role = findByIdOrThrow(id);
        role.setIsActive(false);
        roleRepository.save(role);
    }

    private Role findByIdOrThrow(String id) {
        return roleRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new NotFoundException(StatusMessage.ROLE_NOT_FOUND));
    }

    private RoleResponse toResponse(Role role, List<Permission> permissions) {
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
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