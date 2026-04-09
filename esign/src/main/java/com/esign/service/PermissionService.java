package com.esign.service;

import com.esign.entities.permission.PermissionResponse;

import java.util.List;

public interface PermissionService {
    List<PermissionResponse> getAll();
}
