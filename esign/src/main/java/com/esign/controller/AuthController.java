package com.esign.controller;

import com.esign.constant.ApiUrl;
import com.esign.constant.StatusMessage;
import com.esign.entities.WebResponse;
import com.esign.entities.user.ForgotPasswordRequest;
import com.esign.entities.user.LoginRequest;
import com.esign.entities.user.LoginResponse;
import com.esign.entities.user.ResetPasswordRequest;
import com.esign.exception.BadRequestException;
import com.esign.exception.ValidationCustomException;
import com.esign.helper.Utilities;
import com.esign.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.awt.*;

@RestController
@RequestMapping(ApiUrl.API_URL + ApiUrl.API_AUTH)
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Auth Management API")
public class AuthController {
    private final AuthService authService;
    private final Utilities utilities;

    @Operation(summary = "Login")
    @SecurityRequirement(name = "Authorization")
    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        return utilities.handleRequest(() -> authService.login(request), HttpStatus.OK, StatusMessage.SUCCESS_LOGIN);
    }

    @Operation(summary = "User forgot password")
    @SecurityRequirement(name = "Authorization")
    @PostMapping(path = "/forgot-password", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<String>> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        return utilities.handleRequest(() -> {
            try {
                return authService.forgetPassword(request);
            } catch (ValidationCustomException e) {
                throw new RuntimeException(e);
            }
        }, HttpStatus.OK, "Success");
    }

    @Operation(summary = "User reset password")
    @SecurityRequirement(name = "Authorization")
    @PostMapping(path = "/reset-password", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<String>> resetPassword(@RequestParam("token") String token, @RequestBody ResetPasswordRequest request) {
        return utilities.handleRequest(() -> {
            try {
                return authService.resetPassword(token, request);
            } catch (ValidationCustomException | BadRequestException e) {
                throw new RuntimeException(e);
            }
        }, HttpStatus.OK, "Success");
    }
}
