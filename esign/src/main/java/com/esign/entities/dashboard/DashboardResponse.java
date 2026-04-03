package com.esign.entities.dashboard;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private DashboardSummary summary;
    private List<UrgentDocumentResponse> urgent;
    private List<ActiveDocumentResponse> myActiveDocuments;
    private List<RecentActivityResponse> recentActivity;
}
