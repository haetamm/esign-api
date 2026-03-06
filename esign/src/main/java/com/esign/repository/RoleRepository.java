package com.esign.repository;

import com.esign.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, String> {
    Optional<Role> findByName(String name);
    boolean existsByName(String name);
    Optional<Role> findByIdAndIsActiveTrue(String id);
    List<Role> findAllByIsActiveTrue();
}
