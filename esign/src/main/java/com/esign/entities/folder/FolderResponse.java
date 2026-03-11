package com.esign.entities.folder;

import lombok.*;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FolderResponse {
    private String id;
    private String name;
    private String parentId;
    private String parentName;
    private Boolean isPublic;
    private String requiredRole;
    private List<FolderResponse> children;
    private List<FolderContributorResponse> contributors;
    private String createdAt;
    private String updatedAt;
}
