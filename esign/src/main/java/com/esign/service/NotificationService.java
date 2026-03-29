package com.esign.service;

import com.esign.constant.NotificationType;
import com.esign.entities.notification.NotificationResponse;
import com.esign.model.User;
import org.springframework.data.domain.Page;

public interface NotificationService {
    void send(User user, String title, String message, NotificationType type, String referenceId);
    Page<NotificationResponse> getAll(Integer page, Integer size);
    void markAsRead(String id);
}
