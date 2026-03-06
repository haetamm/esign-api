package com.esign.service;



import com.esign.exception.NotFoundException;
import com.esign.model.User;

public interface UserService {
    User getByUserId(String id) throws NotFoundException;
}
