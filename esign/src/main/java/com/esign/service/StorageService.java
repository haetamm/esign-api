package com.esign.service;

import com.esign.model.Folder;

import java.io.IOException;
import java.nio.file.Path;

public interface StorageService {
    Path getAvatarPath();
    Path getDocumentPath(String category, String documentId) throws IOException;
    String resolveCategory(Folder folder);
    void deleteDocumentFiles(String category, String documentId) throws IOException;
}
