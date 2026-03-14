package com.esign.entities.folder;

import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FolderTrashResponse {
    private String id;
    private String name;
    private String type;
    private String requiredRole;
    private Boolean isPublic;
    private String deletedAt;
    private String deletedBy;
    private Boolean canRestore;
    private String restoreNote;
}
