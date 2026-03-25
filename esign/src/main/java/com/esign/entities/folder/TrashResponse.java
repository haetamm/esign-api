package com.esign.entities.folder;

import com.esign.entities.document.DocumentTrashResponse;
import lombok.*;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TrashResponse {
    private List<FolderTrashResponse> folders;
    private List<DocumentTrashResponse> documents;
}
