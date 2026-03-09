package com.esign.controller;

import com.esign.constant.ApiUrl;
import com.esign.constant.StatusMessage;
import com.esign.entities.PaginationResponse;
import com.esign.entities.WebResponse;
import com.esign.entities.role.*;
import com.esign.exception.BadRequestException;
import com.esign.exception.NotFoundException;
import com.esign.exception.ValidationCustomException;
import com.esign.helper.Utilities;
import com.esign.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiUrl.API_URL + ApiUrl.API_ROLE)
@RequiredArgsConstructor
@Tag(name = "Role", description = "Role Management API")
public class RoleController {

    private final RoleService roleService;
    private final Utilities utilities;

    @Operation(summary = "Create role")
    @SecurityRequirement(name = "Authorization")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<RoleDetailResponse>> create(@RequestBody RoleRequest request) {
        return utilities.handleRequest(
                () -> {
                    try {
                        return roleService.create(request);
                    } catch (NotFoundException | BadRequestException e) {
                        throw new RuntimeException(e);
                    }
                },
                HttpStatus.CREATED,
                StatusMessage.SUCCESS_CREATE
        );
    }

    @Operation(summary = "Get all role")
    @SecurityRequirement(name = "Authorization")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<List<RoleResponse>>> getAll(
            @ParameterObject @ModelAttribute SearchRoleRequest request
    ) {
        Page<RoleResponse> rolePage = roleService.getAll(request);

        PaginationResponse pagination = new PaginationResponse(
                rolePage.getTotalPages(),
                rolePage.getTotalElements(),
                rolePage.getNumber() + 1,
                rolePage.getSize(),
                rolePage.hasNext(),
                rolePage.hasPrevious()
        );

        return utilities.handleRequest(
                rolePage::getContent,
                HttpStatus.OK,
                StatusMessage.SUCCESS_RETRIEVE,
                pagination
        );
    }

    @Operation(summary = "Get role by id")
    @SecurityRequirement(name = "Authorization")
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<RoleDetailResponse>> getById(@PathVariable String id) {
        return utilities.handleRequest(
                () -> {
                    try {
                        return roleService.getById(id);
                    } catch (NotFoundException e) {
                        throw new RuntimeException(e);
                    }
                },
                HttpStatus.OK,
                StatusMessage.SUCCESS_RETRIEVE
        );
    }

    @Operation(summary = "Update role")
    @SecurityRequirement(name = "Authorization")
    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<RoleDetailResponse>> update(@PathVariable String id, @RequestBody UpdateRoleRequest request) {
        return utilities.handleRequest(
                () -> {
                    try {
                        return roleService.update(id, request);
                    } catch (NotFoundException | ValidationCustomException e) {
                        throw new RuntimeException(e);
                    }
                },
                HttpStatus.OK,
                StatusMessage.SUCCESS_UPDATE
        );
    }

    @Operation(summary = "Activate or deactivate role")
    @SecurityRequirement(name = "Authorization")
    @PatchMapping(path = "/{id}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<String>> toggleStatus(@PathVariable String id) {
        String message = roleService.toggleStatus(id);
        return utilities.handleRequest(() -> null, HttpStatus.OK, message);
    }
}