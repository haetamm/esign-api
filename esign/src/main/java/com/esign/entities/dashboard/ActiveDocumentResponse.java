package com.esign.entities.dashboard;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveDocumentResponse {
    private String id;
    private String title;
    private String fileName;
    private Long fileSize;
    private String status;
    private String folderName;
    private Integer totalContributors;
    private Integer signedContributors;
    private Integer pendingContributors;
    private String deadline;
    private Boolean isOverdue;
    private String updatedAt;
}
