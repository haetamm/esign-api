package com.esign.repository;

import com.esign.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUsername(String username);
    Boolean existsByEmail(String email);
    Boolean existsByUsername(String username);
    List<User> findAllByIsEnableTrue();
    Optional<User> findByIdAndIsEnableTrue(String id);
}
