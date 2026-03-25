package com.esign.entities.document;

import com.esign.constant.DocumentStatus;
import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DocumentTrashResponse {
    private String id;
    private String name;
    private String type;
    private String requiredRole;
    private DocumentStatus documentStatus;
    private String deletedAt;
    private String deletedBy;
    private Boolean canRestore;
    private String restoreNote;
}
