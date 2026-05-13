package com.esign.controller;

import com.esign.constant.ApiUrl;
import com.esign.constant.StatusMessage;
import com.esign.entities.WebResponse;
import com.esign.entities.profile.ChangePasswordRequest;
import com.esign.entities.profile.UpdateProfileRequest;
import com.esign.entities.profile.UploadAvatarRequest;
import com.esign.entities.role.RoleDetailResponse;
import com.esign.entities.user.*;
import com.esign.exception.BadRequestException;
import com.esign.exception.NotFoundException;
import com.esign.exception.ValidationCustomException;
import com.esign.helper.Utilities;
import com.esign.service.AuthService;
import com.esign.service.ProfileService;
import com.esign.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping(ApiUrl.API_URL + ApiUrl.API_PROFILE)
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Profile Management API")
public class ProfileController {
    private final UserService userService;
    private final ProfileService profileService;
    private final Utilities utilities;
    private final AuthService authService;

    @Operation(summary = "Update Profile")
    @SecurityRequirement(name = "Authorization")
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<DetailResponse>> updateCurrentUser(@RequestBody UpdateProfileRequest request) {
        return utilities.handleRequest(() -> {
            try {
                return userService.updateCurrentUser(request);
            } catch (ValidationCustomException | BadRequestException e) {
                throw new RuntimeException(e);
            }
        }, HttpStatus.OK, StatusMessage.SUCCESS_UPDATE);
    }

    @Operation(summary = "Get Profile")
    @SecurityRequirement(name = "Authorization")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<DetailResponse>> getCurrentUser() {
        return utilities.handleRequest(
                () -> {
                    try {
                        return userService.getCurrentUser();
                    } catch (NotFoundException e) {
                        throw new RuntimeException(e);
                    }
                },
                HttpStatus.OK,
                StatusMessage.SUCCESS_RETRIEVE
        );
    }

    @Operation(summary = "Get role permission")
    @SecurityRequirement(name = "Authorization")
    @GetMapping(path = "/permission", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<RoleDetailResponse>> getRolePermission() {
        return utilities.handleRequest(
                () -> {
                    try {
                        return profileService.getRolePermission();
                    } catch (NotFoundException e) {
                        throw new RuntimeException(e);
                    }
                },
                HttpStatus.OK,
                StatusMessage.SUCCESS_RETRIEVE
        );
    }

    @Operation(summary = "Change Password")
    @SecurityRequirement(name = "Authorization")
    @PostMapping(path = "/credential", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<String>> changePassword(@RequestBody ChangePasswordRequest request) {
        return utilities.handleRequest(() -> {
            try {
                return profileService.changePassword(request);
            } catch (ValidationCustomException e) {
                throw new RuntimeException(e);
            }
        }, HttpStatus.OK, StatusMessage.SUCCESS_UPDATE);
    }

    @Operation(summary = "Logout")
    @SecurityRequirement(name = "Authorization")
    @PostMapping(path = "/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<String>> logout(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        return utilities.handleRequest(() -> {
            authService.logout(bearerToken);
            return StatusMessage.SUCCESS_LOGOUT;
        }, HttpStatus.OK, StatusMessage.SUCCESS_LOGOUT);
    }

    @Operation(summary = "Upload profile image")
    @SecurityRequirement(name = "Authorization")
    @PostMapping(
            path = "/avatar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<WebResponse<String>> uploadAvatar(
            @Valid @ModelAttribute UploadAvatarRequest request
    ) {
        return utilities.handleRequest(() -> {
            try {
                return profileService.uploadAvatar(request);
            } catch (NotFoundException | IOException e) {
                throw new RuntimeException(e);
            }
        }, HttpStatus.OK, StatusMessage.SUCCESS_UPDATE);
    }

    @Operation(summary = "Get image by id")
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/{profile_id}/avatar")
    public ResponseEntity<Resource> getAvatarById(@PathVariable String profile_id) throws IOException, NotFoundException {
        Resource image = profileService.getByProfileId(profile_id);
        Path filePath = image.getFile().toPath();
        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        String headerValue = "attachment; filename=\"" + image.getFilename() + "\"";
        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_DISPOSITION, headerValue)
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(image);
    }

}
