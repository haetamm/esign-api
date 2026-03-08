package com.esign.service.impl;

import com.esign.constant.RoleName;
import com.esign.constant.StatusMessage;
import com.esign.entities.user.*;
import com.esign.exception.NotFoundException;
import com.esign.exception.ValidationCustomException;
import com.esign.helper.Utilities;
import com.esign.model.*;
import com.esign.repository.ProfileRepository;
import com.esign.repository.UserRepository;
import com.esign.repository.UserRoleRepository;
import com.esign.service.RoleService;
import com.esign.service.UserService;

import com.esign.specification.UserSpecification;
import com.esign.validation.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final UserRoleRepository userRoleRepository;
    private final ValidationUtil validationUtil;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final UserSpecification userSpecification;
    private final Utilities utilities;

    @Transactional(readOnly = true)
    @Override
    public User getByUserId(String id) throws NotFoundException {
        return findById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public RegisterResponse createUser(RegisterRequest request) throws ValidationCustomException {
        validationUtil.validate(request);
        Role role = getAndValidateRole(request.getRole_id());

        Profile profile = profileRepository.save(Profile.builder()
                .name(request.getName())
                .gender(request.getGender())
                .build());

        User user = userRepository.save(User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .profile(profile)
                .isEnable(true)
                .build());

        userRoleRepository.save(UserRole.builder()
                .user(user)
                .role(role)
                .build());

        List<String> roles = userRoleRepository.findByUser(user)
                .stream()
                .map(userRole -> userRole.getRole().getName())
                .toList();

        return RegisterResponse.builder()
                .username(user.getUsername())
                .roles(roles)
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public DetailResponse updateUser(UpdateRequest request, String userId) throws ValidationCustomException {
        validationUtil.validate(request);
        Role role = getAndValidateRole(request.getRole_id());

        User user = findById(userId);
        userRoleRepository.deleteAllByUser(user);
        userRoleRepository.save(UserRole.builder()
                .user(user)
                .role(role)
                .build());

        updateUserData(request, user);
        userRepository.save(user);
        Profile profile = profileRepository.save(user.getProfile());

        return setDetailResponse(user, profile);
    }

    @Transactional(readOnly = true)
    @Override
    public DetailResponse getById(String id) throws NotFoundException {
        User user = findById(id);
        Profile profile = user.getProfile();
        return setDetailResponse(user, profile);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<UserResponse> getAll(SearchUserRequest request) {
        Specification<User> spec = userSpecification.specification(request);

        Pageable pageable = utilities.buildPageable(
                request.getPage(),
                request.getSize(),
                request.getSortBy(),
                request.getDirection(),
                "createdAt"
        );

        return userRepository.findAll(spec, pageable)
                .map(this::setUserResponse);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String toggleStatus(String id) throws NotFoundException {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(StatusMessage.USER_NOT_FOUND));
        user.setIsEnable(!user.getIsEnable());
        userRepository.save(user);
        return user.getIsEnable() ? StatusMessage.SUCCESS_ACTIVE : StatusMessage.SUCCESS_INACTIVE;
    }

    private UserResponse setUserResponse(User user) {
        Profile profile = user.getProfile();
        return UserResponse.builder()
                .id(user.getId())
                .name(profile.getName())
                .phone(profile.getPhone())
                .address(profile.getAddress())
                .gender(profile.getGender())
                .email(user.getEmail())
                .isEnable(user.getIsEnable())
                .build();
    }

    private User findById(String id) throws NotFoundException {
        return userRepository.findByIdAndIsEnableTrue(id).orElseThrow(() -> new NotFoundException(StatusMessage.USER_NOT_FOUND));
    }

    private void updateUserData(UpdateRequest request, User user) throws ValidationCustomException {
        updateUsernameIfChange(request.getUsername(), user);
        updateEmailIfChange(request.getEmail(), user);

        Profile profile = user.getProfile();
        profile.setName(request.getName());
        profile.setPhone(request.getPhone());
        profile.setAddress(request.getAddress());
        profile.setGender(request.getGender());
        profile.setBirthDate(LocalDate.parse(request.getBirthDate()));
        profile.setBirthPlace(request.getBirthPlace());
        profile.setReligion(request.getReligion());
    }

    private void updateUsernameIfChange(String newUsername, User user) throws ValidationCustomException {
        if (newUsername != null && !newUsername.isBlank() && !newUsername.equals(user.getUsername())) {
            if (userRepository.existsByUsername(newUsername)) {
                throw new ValidationCustomException(StatusMessage.USERNAME_BEEN_TAKEN, "username");
            }
            user.setUsername(newUsername);
        }
    }

    private void updateEmailIfChange(String newEmail, User user) throws ValidationCustomException {
        if (newEmail != null && !newEmail.isBlank() && !newEmail.equals(user.getEmail())) {
            if (userRepository.existsByEmail(newEmail)) {
                throw new ValidationCustomException(StatusMessage.EMAIL_BEEN_TAKEN, "email");
            }
            user.setEmail(newEmail);
        }
    }

    private Role getAndValidateRole(String roleId) throws ValidationCustomException {
        Role role = roleService.getEntityById(roleId);
        if (role.getName().equalsIgnoreCase(RoleName.SUPER_ADMIN)) {
            throw new ValidationCustomException("Cannot assign SUPER_ADMIN role", "role_id");
        }
        return role;
    }

    private DetailResponse setDetailResponse(User user, Profile profile) {
        List<String> roles = userRoleRepository.findByUser(user)
                .stream()
                .map(userRole -> userRole.getRole().getName())
                .toList();

        return DetailResponse.builder()
                .name(profile.getName())
                .phone(profile.getPhone())
                .birthPlace(profile.getBirthPlace())
                .birthDate(String.valueOf(profile.getBirthDate()))
                .religion(profile.getReligion())
                .gender(profile.getGender())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roles)
                .createdAt(String.valueOf(user.getCreatedAt()))
                .updatedAt(String.valueOf(user.getUpdatedAt()))
                .build();
    }
}
