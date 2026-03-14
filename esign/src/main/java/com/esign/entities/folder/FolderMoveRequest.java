package com.esign.entities.folder;

import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FolderMoveRequest {
    private String parentId; // null = pindah ke root
}
