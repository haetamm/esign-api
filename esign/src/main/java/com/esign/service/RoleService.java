package com.esign.service;

import com.esign.entities.role.RoleRequest;
import com.esign.entities.role.RoleResponse;
import com.esign.exception.BadRequestException;
import com.esign.exception.NotFoundException;

import java.util.List;

public interface RoleService {
    RoleResponse create(RoleRequest request) throws NotFoundException, BadRequestException;
    List<RoleResponse> getAll();
    RoleResponse getById(String id) throws NotFoundException;
    RoleResponse update(String id, RoleRequest request) throws NotFoundException;
    void delete(String id) throws NotFoundException;
}