package com.esign.repository;

import com.esign.model.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<Profile, String> {
    Boolean existsByName(String name);
}
