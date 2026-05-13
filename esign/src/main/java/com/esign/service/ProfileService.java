package com.esign.service;

import com.esign.entities.profile.ChangePasswordRequest;
import com.esign.entities.profile.UploadAvatarRequest;
import com.esign.entities.role.RoleDetailResponse;
import com.esign.entities.user.DetailResponse;
import com.esign.exception.NotFoundException;
import com.esign.exception.ValidationCustomException;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.net.MalformedURLException;

public interface ProfileService {
    String changePassword(ChangePasswordRequest request) throws ValidationCustomException;
    String uploadAvatar(UploadAvatarRequest request) throws NotFoundException, IOException;
    Resource getByProfileId(String id) throws NotFoundException, MalformedURLException;
    RoleDetailResponse getRolePermission() throws NotFoundException;
}
