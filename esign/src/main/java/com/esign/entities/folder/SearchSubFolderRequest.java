package com.esign.entities.folder;

import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchSubFolderRequest {
    private String name;      // filter nama
    private String filter;    // mine, contributor, null = semua
}

