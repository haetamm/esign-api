package com.esign.repository;

import com.esign.constant.ActionType;
import com.esign.model.Role;
import com.esign.model.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, String> {
    boolean existsByRoleNameInAndPermissionUrlAndPermissionAction(
            List<String> roleNames,
            String url,
            ActionType action
    );
    List<RolePermission> findByRole(Role role);
    void deleteByRole(Role role);
}

