package com.esign.service.impl;

import com.esign.entities.permission.PermissionResponse;
import com.esign.model.Permission;
import com.esign.repository.PermissionRepository;
import com.esign.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {
    private final PermissionRepository permissionRepository;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public List<PermissionResponse> getAll() {
        List<Permission> permissions = permissionRepository.findAll();

        return permissions.stream()
                .map(permission -> PermissionResponse.builder()
                .id(permission.getId())
                .url(permission.getUrl())
                .action(permission.getAction())
                .build())
                .collect(Collectors.toList());
    }
}
