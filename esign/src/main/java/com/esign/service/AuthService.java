package com.esign.service;


import com.esign.entities.user.ForgotPasswordRequest;
import com.esign.entities.user.LoginRequest;
import com.esign.entities.user.LoginResponse;
import com.esign.entities.user.ResetPasswordRequest;
import com.esign.exception.BadRequestException;
import com.esign.exception.ValidationCustomException;
import com.esign.model.User;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    String forgetPassword(ForgotPasswordRequest request) throws ValidationCustomException;
    String resetPassword(String token, ResetPasswordRequest request) throws ValidationCustomException, BadRequestException;
    User getAuthenticatedUser();
}
