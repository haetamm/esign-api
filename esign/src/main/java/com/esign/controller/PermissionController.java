package com.esign.controller;

import com.esign.constant.ApiUrl;
import com.esign.constant.StatusMessage;
import com.esign.entities.WebResponse;
import com.esign.entities.permission.PermissionResponse;
import com.esign.helper.Utilities;
import com.esign.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiUrl.API_URL + ApiUrl.API_PERMISSION)
@RequiredArgsConstructor
@Tag(name = "Permission", description = "Permission Management API")
public class PermissionController {
    private final PermissionService permissionService;
    private final Utilities utilities;

    @Operation(summary = "get all permission")
    @SecurityRequirement(name = "Authorization")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<List<PermissionResponse>>> getAll() {
        return utilities.handleRequest(permissionService::getAll, HttpStatus.OK, StatusMessage.SUCCESS_RETRIEVE);
    }
}
