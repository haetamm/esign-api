package com.esign.service.impl;

import com.esign.constant.NotificationType;
import com.esign.constant.StatusMessage;
import com.esign.entities.notification.NotificationResponse;
import com.esign.exception.NotFoundException;
import com.esign.helper.Utilities;
import com.esign.model.Notification;
import com.esign.model.User;
import com.esign.repository.NotificationRepository;
import com.esign.service.AuthService;
import com.esign.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final AuthService authService;
    private final Utilities utilities;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void send(User user, String title, String message, NotificationType type, String referenceId) {
        Notification notification = notificationRepository.save(
                Notification.builder()
                        .user(user)
                        .title(title)
                        .message(message)
                        .type(type)
                        .referenceId(referenceId)
                        .build()
        );

        NotificationResponse response = toResponse(notification);
        simpMessagingTemplate.convertAndSendToUser(
                user.getId(),
                "/queue/notifications",
                response
        );
    }

    @Transactional(readOnly = true)
    @Override
    public Page<NotificationResponse> getAll(Integer page, Integer size) {
        User user = authService.getAuthenticatedUser();
        Pageable pageable = utilities.buildPageable(page, size, null, "DESC", "createdAt");
        return notificationRepository.findAllByUser(user, pageable)
                .map(this::toResponse);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void markAsRead(String id) {
        User user = authService.getAuthenticatedUser();
        Notification notification = notificationRepository.findByIdAndUser(id, user).orElseThrow(() -> new NotFoundException(StatusMessage.NOTIFICATION_NOT_FOUND));
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .referenceId(notification.getReferenceId())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt().toString())
                .build();
    }
}
