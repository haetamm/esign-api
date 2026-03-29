package com.esign.controller;

import com.esign.constant.ApiUrl;
import com.esign.constant.StatusMessage;
import com.esign.entities.PaginationResponse;
import com.esign.entities.WebResponse;
import com.esign.entities.notification.NotificationResponse;
import com.esign.helper.Utilities;
import com.esign.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(ApiUrl.API_URL + ApiUrl.API_NOTIFICATION)
@RequiredArgsConstructor
@Tag(name = "Notification", description = "Notification API")
public class NotificationController {

    private final NotificationService notificationService;
    private final Utilities utilities;

    @Operation(summary = "Get all notifications")
    @SecurityRequirement(name = "Authorization")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<List<NotificationResponse>>> getAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<NotificationResponse> notifPage = notificationService.getAll(page, size);

        PaginationResponse pagination = new PaginationResponse(
                notifPage.getTotalPages(),
                notifPage.getTotalElements(),
                notifPage.getNumber() + 1,
                notifPage.getSize(),
                notifPage.hasNext(),
                notifPage.hasPrevious()
        );

        return utilities.handleRequest(
                notifPage::getContent,
                HttpStatus.OK,
                StatusMessage.SUCCESS_RETRIEVE,
                pagination
        );
    }
    @Operation(summary = "Mark notification as read")
    @SecurityRequirement(name = "Authorization")
    @PatchMapping(path = "/{id}/read", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<Void>> markAsRead(@PathVariable String id) {
        return utilities.handleRequest(() -> {
            notificationService.markAsRead(id);
            return null;
        }, HttpStatus.OK, StatusMessage.SUCCESS_UPDATE);
    }

}
