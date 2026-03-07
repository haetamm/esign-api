package com.esign.service;



import com.esign.entities.user.*;
import com.esign.exception.BadRequestException;
import com.esign.exception.NotFoundException;
import com.esign.exception.ValidationCustomException;
import com.esign.model.User;

import java.util.List;

public interface UserService {
    User getByUserId(String id) throws NotFoundException;
    RegisterResponse createUser(RegisterRequest request) throws BadRequestException, ValidationCustomException;
    DetailResponse updateUser(UpdateRequest request, String userId) throws BadRequestException, ValidationCustomException;
    DetailResponse getById(String id) throws NotFoundException;
    List<UserResponse> getAll();
    void deleteUser(String id) throws NotFoundException;
}
