package com.esign.entities.document;

import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DocumentMoveRequest {
    private String folderId;
}
