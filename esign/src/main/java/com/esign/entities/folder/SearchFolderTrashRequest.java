package com.esign.entities.folder;

import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchFolderTrashRequest {
    private String name;
    private String type; // public / role
}
