package com.esign.entities.dashboard;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummary {
    private Integer totalMyDocuments;
    private Integer draft;
    private Integer waitingSignature;
    private Integer inProgress;
    private Integer completed;
    private Integer rejected;
    private Integer needMySignature;    // as contributor, status PENDING
    private Integer overdue;            // deadline < now, status != COMPLETED
}