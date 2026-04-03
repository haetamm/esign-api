package com.esign.controller;

import com.esign.constant.ApiUrl;
import com.esign.constant.StatusMessage;
import com.esign.entities.WebResponse;
import com.esign.entities.dashboard.DashboardResponse;
import com.esign.helper.Utilities;
import com.esign.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiUrl.API_URL + ApiUrl.API_DASHBOARD)
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard API")
public class DashboardController {

    private final DashboardService dashboardService;
    private final Utilities utilities;

    @Operation(
            summary = "Get dashboard data",
            description = "Returns summary stats, urgent documents (deadline within 2 days), " +
                    "active documents (IN_PROGRESS & WAITING_SIGNATURE), " +
                    "and recent activity (last 3 days) for the authenticated user."
    )
    @SecurityRequirement(name = "Authorization")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<DashboardResponse>> getDashboard() {
        return utilities.handleRequest(
                dashboardService::getDashboard,
                HttpStatus.OK,
                StatusMessage.SUCCESS_RETRIEVE
        );
    }

}
