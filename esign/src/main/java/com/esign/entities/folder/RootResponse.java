package com.esign.entities.folder;

import com.esign.entities.document.DocumentResponse;
import lombok.*;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RootResponse {
    private List<FolderResponse> folders;
    private List<DocumentResponse> documents;
}
