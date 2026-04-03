package com.esign.entities.dashboard;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrgentDocumentResponse {
    private String id;
    private String title;
    private String fileName;
    private String documentStatus;
    private String urgentType;          // "NEED_MY_SIGNATURE" atau "WAITING_OTHERS"
    private String ownerName;
    private Integer pendingContributors; // kalau urgentType = WAITING_OTHERS
    private String deadline;
    private Boolean isOverdue;
}
