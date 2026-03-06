package com.esign.repository;

import com.esign.constant.ActionType;
import com.esign.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PermissionRepository extends JpaRepository<Permission, String> {
    boolean existsByUrlAndAction(String url, ActionType action);
    List<Permission> findAllByIdIn(List<String> ids);
}
