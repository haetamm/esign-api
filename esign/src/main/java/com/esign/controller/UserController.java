package com.esign.controller;

import com.esign.constant.ApiUrl;
import com.esign.constant.StatusMessage;
import com.esign.entities.PaginationResponse;
import com.esign.entities.WebResponse;
import com.esign.entities.user.*;
import com.esign.exception.BadRequestException;
import com.esign.exception.NotFoundException;
import com.esign.exception.ValidationCustomException;
import com.esign.helper.Utilities;
import com.esign.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiUrl.API_URL + ApiUrl.API_USER)
@RequiredArgsConstructor
@Tag(name = "User", description = "User Management API")
public class UserController {
    private final UserService userService;
    private final Utilities utilities;

    @Operation(summary = "Register User")
    @SecurityRequirement(name = "Authorization")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<RegisterResponse>> createUser(@RequestBody RegisterRequest request) {
        return utilities.handleRequest(() -> {
            try {
                return userService.createUser(request);
            } catch (ValidationCustomException | BadRequestException e) {
                throw new RuntimeException(e);
            }
        }, HttpStatus.CREATED, StatusMessage.SUCCESS_CREATE);
    }

    @Operation(summary = "Update User By Id")
    @SecurityRequirement(name = "Authorization")
    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<DetailResponse>> updateUser(@RequestBody UpdateUserRequest request, @PathVariable String id) {
        return utilities.handleRequest(() -> {
            try {
                return userService.updateUser(request, id);
            } catch (ValidationCustomException | BadRequestException e) {
                throw new RuntimeException(e);
            }
        }, HttpStatus.OK, StatusMessage.SUCCESS_UPDATE);
    }

    @Operation(summary = "Get User by id")
    @SecurityRequirement(name = "Authorization")
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<DetailResponse>> getById(@PathVariable String id) {
        return utilities.handleRequest(
                () -> {
                    try {
                        return userService.getById(id);
                    } catch (NotFoundException e) {
                        throw new RuntimeException(e);
                    }
                },
                HttpStatus.OK,
                StatusMessage.SUCCESS_RETRIEVE
        );
    }

    @Operation(summary = "Get all user")
    @SecurityRequirement(name = "Authorization")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<List<UserResponse>>> getAll(
            @ParameterObject @ModelAttribute SearchUserRequest request
    ) {
        Page<UserResponse> userPage = userService.getAll(request);

        PaginationResponse pagination = new PaginationResponse(
                userPage.getTotalPages(),
                userPage.getTotalElements(),
                userPage.getNumber() + 1,
                userPage.getSize(),
                userPage.hasNext(),
                userPage.hasPrevious()
        );

        return utilities.handleRequest(
                userPage::getContent,
                HttpStatus.OK,
                StatusMessage.SUCCESS_RETRIEVE,
                pagination
        );
    }

    @Operation(summary = "Activate or deactivate user")
    @SecurityRequirement(name = "Authorization")
    @PatchMapping(path = "/{id}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<String>> toggleStatus(@PathVariable String id) throws AccessDeniedException {
        String message = userService.toggleStatus(id);
        return utilities.handleRequest(() -> null, HttpStatus.OK, message);
    }

}
