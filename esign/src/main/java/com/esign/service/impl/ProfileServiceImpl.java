package com.esign.service.impl;

import com.esign.constant.StatusMessage;
import com.esign.entities.profile.ChangePasswordRequest;
import com.esign.entities.profile.UploadAvatarRequest;
import com.esign.exception.NotFoundException;
import com.esign.exception.ValidationCustomException;
import com.esign.model.Profile;
import com.esign.model.User;
import com.esign.repository.ProfileRepository;
import com.esign.repository.UserRepository;
import com.esign.service.AuthService;
import com.esign.service.ProfileService;
import com.esign.service.StorageService;
import com.esign.service.UserService;
import com.esign.validation.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {
    private final ValidationUtil validationUtil;
    private final PasswordEncoder passwordEncoder;
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final StorageService storageService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Resource getByProfileId(String id) throws NotFoundException, MalformedURLException {
        Profile profile = findById(id);
        Path filePath = Paths.get(profile.getAvatar());

        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
            throw new NotFoundException("File not found or not readable: " + profile.getAvatar());
        }

        return new UrlResource(filePath.toUri());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String changePassword(ChangePasswordRequest request) throws ValidationCustomException {
        validationUtil.validate(request);

        User user = authService.getAuthenticatedUser();
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new ValidationCustomException("Password incorrect", "oldPassword");
        }

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        return "Password successfully updated";
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String uploadAvatar(UploadAvatarRequest request) throws NotFoundException, IOException {

        User user = authService.getAuthenticatedUser();
        Profile profile = user.getProfile();

        // hapus file lama jika ada
        if (profile.getAvatar() != null) {
            Path oldFile = Paths.get(profile.getAvatar());
            Files.deleteIfExists(oldFile);
        }

        // simpan file baru
        MultipartFile file = request.getAvatar();
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = storageService.getAvatarPath().resolve(fileName);
        Files.copy(file.getInputStream(), filePath);

        profile.setAvatar(filePath.toString());
        profileRepository.save(profile);

        return filePath.toString();
    }

    private Profile findById(String id) throws NotFoundException {
        return profileRepository.findById(id).orElseThrow(() -> new NotFoundException(StatusMessage.AVATAR_NOT_FOUND));
    }

}
