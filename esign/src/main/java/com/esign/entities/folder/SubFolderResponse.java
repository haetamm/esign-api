package com.esign.entities.folder;

import com.esign.entities.document.DocumentResponse;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class SubFolderResponse extends FolderResponse {
    private List<FolderResponse> children;
    private List<DocumentResponse> documents;
}
