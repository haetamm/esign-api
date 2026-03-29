package com.esign.entities.notification;

import com.esign.constant.NotificationType;
import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationResponse {
    private String id;
    private String title;
    private String message;
    private NotificationType type;
    private String referenceId;
    private Boolean isRead;
    private String createdAt;
}
