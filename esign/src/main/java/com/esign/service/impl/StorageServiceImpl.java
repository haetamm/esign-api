package com.esign.service.impl;

import com.esign.model.Folder;
import com.esign.service.StorageService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

    @Value("${esign-api.image.path}")
    private String pathImage;

    @Value("${esign-api.document.path}")
    private String pathDocument;

    private Path avatarPath;
    private Path documentPath;

    @PostConstruct
    public void initPath() throws IOException {
        avatarPath = Paths.get(pathImage);
        if (!Files.exists(avatarPath)) {
            try {
                Files.createDirectories(avatarPath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create image directory: " + avatarPath, e);
            }
        }

        documentPath = Paths.get(pathDocument);
        if (!Files.exists(documentPath)) {
            try {
                Files.createDirectories(documentPath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create document directory: " + documentPath, e);
            }
        }
    }

    @Override
    public Path getAvatarPath() {
        return avatarPath;
    }

    @Override
    public Path getDocumentPath(String category, String documentId) throws IOException {
        // category = "public" atau nama role (FINANCE, HR, dll)
        Path categoryPath = documentPath.resolve(category).resolve(documentId);
        if (!Files.exists(categoryPath)) {
            Files.createDirectories(categoryPath);
        }
        return categoryPath;
    }

    @Override
    public String resolveCategory(Folder folder) {
        if (folder == null || folder.getRequiredRole() == null) {
            return "public"; // folderId=null atau folder umum
        }
        return folder.getRequiredRole().getName(); // nama role → FINANCE, HR, dll
    }

    @Override
    public void deleteDocumentFiles(String category, String documentId) throws IOException {
        Path documentDir = documentPath.resolve(category).resolve(documentId);
        if (Files.exists(documentDir)) {
            try (Stream<Path> walk = Files.walk(documentDir)) {
                walk.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        }
    }
}
