package com.esign.service;

import com.esign.entities.role.RoleRequest;
import com.esign.entities.role.RoleDetailResponse;
import com.esign.entities.role.RoleResponse;
import com.esign.entities.role.SearchRoleRequest;
import com.esign.exception.BadRequestException;
import com.esign.exception.NotFoundException;
import com.esign.model.Role;
import org.springframework.data.domain.Page;

public interface RoleService {
    RoleDetailResponse create(RoleRequest request) throws NotFoundException, BadRequestException;
    Page<RoleResponse> getAll(SearchRoleRequest name);
    RoleDetailResponse getById(String id) throws NotFoundException;
    RoleDetailResponse update(String id, RoleRequest request) throws NotFoundException;
    String toggleStatus(String id) throws NotFoundException;
    Role getEntityById(String id);
}