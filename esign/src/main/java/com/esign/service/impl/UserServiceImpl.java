package com.esign.service.impl;

import com.esign.constant.StatusMessage;
import com.esign.exception.NotFoundException;
import com.esign.model.User;
import com.esign.repository.UserRepository;
import com.esign.service.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    @Override
    public User getByUserId(String id) throws NotFoundException {
        return findById(id);
    }

    private User findById(String id) throws NotFoundException {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException(StatusMessage.USER_NOT_FOUND));
    }
}
