package com.esign.entities.document;

import com.esign.constant.DocumentStatus;
import lombok.*;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DocumentResponse {
    private String id;
    private String title;
    private String fileName;
    private Long fileSize;
    private DocumentStatus status;
    private String folderId;
    private String folderName;
    private String ownerUsername;
    private List<DocumentContributorResponse> contributors;
    private String deadline;
    private String createdAt;
    private String updatedAt;
}
