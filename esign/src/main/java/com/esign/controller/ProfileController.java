package com.esign.controller;

import com.esign.constant.ApiUrl;
import com.esign.constant.StatusMessage;
import com.esign.entities.WebResponse;
import com.esign.entities.profile.UpdateProfileRequest;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiUrl.API_URL + ApiUrl.API_PROFILE)
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Profile Management API")
public class ProfileController {
    private final UserService userService;
    private final Utilities utilities;

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

}
