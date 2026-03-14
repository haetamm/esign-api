package com.esign.entities.folder;

import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchFolderRequest {
    private String name;      // filter nama
    private Boolean isRole;   // false/null = root umum, true = root role
    private String filter;    // mine, contributor, null = semua
}

