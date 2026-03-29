package com.esign.repository;

import com.esign.model.Notification;
import com.esign.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, String> {
    Page<Notification> findAllByUser(User user, Pageable pageable);
    Optional<Notification> findByIdAndUser(String id, User user);
}
