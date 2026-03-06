package com.esign.service;


import com.esign.entities.user.LoginRequest;
import com.esign.entities.user.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
}
