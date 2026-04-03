package com.esign.entities.dashboard;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentActivityResponse {
    private String documentId;
    private String documentTitle;
    private String activityType;
    private String description;
    private String triggeredBy;
    private String createdAt;
}
