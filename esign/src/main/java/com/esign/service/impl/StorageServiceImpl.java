package com.esign.service.impl;

import com.esign.service.StorageService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

    @Value("${esign-api.image.path}")
    private String pathImage;

    private Path avatarPath;

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
    }

    @Override
    public Path getAvatarPath() {
        return avatarPath;
    }
}
