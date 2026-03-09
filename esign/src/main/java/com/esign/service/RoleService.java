package com.esign.service;

import com.esign.entities.role.*;
import com.esign.exception.BadRequestException;
import com.esign.exception.NotFoundException;
import com.esign.exception.ValidationCustomException;
import com.esign.model.Role;
import org.springframework.data.domain.Page;

public interface RoleService {
    RoleDetailResponse create(RoleRequest request) throws NotFoundException, BadRequestException;
    Page<RoleResponse> getAll(SearchRoleRequest name);
    RoleDetailResponse getById(String id) throws NotFoundException;
    RoleDetailResponse update(String id, UpdateRoleRequest request) throws NotFoundException, ValidationCustomException;
    String toggleStatus(String id) throws NotFoundException;
    Role getEntityById(String id);
}