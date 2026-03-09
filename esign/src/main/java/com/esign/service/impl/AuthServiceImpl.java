package com.esign.service.impl;

import com.esign.constant.ActionType;
import com.esign.constant.ApiUrl;
import com.esign.constant.RoleName;
import com.esign.entities.user.ForgotPasswordRequest;
import com.esign.entities.user.LoginRequest;
import com.esign.entities.user.LoginResponse;
import com.esign.entities.user.ResetPasswordRequest;
import com.esign.exception.BadRequestException;
import com.esign.exception.ValidationCustomException;
import com.esign.model.*;
import com.esign.repository.*;
import com.esign.model.*;
import com.esign.repository.*;
import com.esign.service.AuthService;
import com.esign.service.JwtService;
import com.esign.validation.ValidationUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final ValidationUtil validationUtil;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final ProfileRepository profileRepository;
    private final JavaMailSender mailSender;

    @Value("${esign_api.super-admin.name}")
    private String superAdminName;

    @Value("${esign_api.super-admin.username}")
    private String superAdminUsername;

    @Value("${esign_api.super-admin.email}")
    private String superAdminEmail;

    @Value("${esign_api.super-admin.password}")
    private String superAdminPassword;

    @Transactional(rollbackFor = Exception.class)
    @PostConstruct
    public void init() {
        seedPermissions();
        seedRoles();
        seedSuperAdmin();
    }

    private void seedPermissions() {
        List<String> urls = List.of(
                ApiUrl.API_URL + ApiUrl.API_ROLE,
                ApiUrl.API_URL + ApiUrl.API_USER,
                ApiUrl.API_URL + ApiUrl.API_PROFILE
        );

        for (String url : urls) {
            for (ActionType action : ActionType.values()) {
                boolean exists = permissionRepository.existsByUrlAndAction(url, action);
                if (!exists) {
                    permissionRepository.save(
                            Permission.builder()
                                    .url(url)
                                    .action(action)
                                    .build()
                    );
                }
            }
        }
    }

    private void seedRoles() {
        // buat role jika belum ada
        Role superAdminRole = roleRepository.findByName(RoleName.SUPER_ADMIN)
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .name(RoleName.SUPER_ADMIN)
                                .build()
                ));

        List<Permission> allPermissions = permissionRepository.findAll();
        List<Permission> existingPermissions = rolePermissionRepository.findByRole(superAdminRole)
                .stream()
                .map(RolePermission::getPermission)
                .toList();

        List<Permission> newPermissions = allPermissions.stream()
                .filter(p -> !existingPermissions.contains(p))
                .toList();

        for (Permission permission : newPermissions) {
            rolePermissionRepository.save(
                    RolePermission.builder()
                            .role(superAdminRole)
                            .permission(permission)
                            .build()
            );
        }
    }

    private void seedSuperAdmin() {
        boolean exists = userRepository.existsByUsername(superAdminUsername);
        if (exists) return;

        Profile profile = profileRepository.save(Profile.builder()
                .name(superAdminName)
                .gender("Laki Laki")
                .build());

        User superAdmin = userRepository.save(
                User.builder()
                        .username(superAdminUsername)
                        .email(superAdminEmail)
                        .password(passwordEncoder.encode(superAdminPassword))
                        .profile(profile)
                        .isEnable(true)
                        .build()
        );

        Role superAdminRole = roleRepository.findByName("SUPER_ADMIN")
                .orElseThrow(() -> new RuntimeException("Role SUPER_ADMIN not found"));

        userRoleRepository.save(
                UserRole.builder()
                        .user(superAdmin)
                        .role(superAdminRole)
                        .build()
        );
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public LoginResponse login(LoginRequest request) {
        validationUtil.validate(request);
        Authentication authentication = new UsernamePasswordAuthenticationToken(request.getUsername(),
                request.getPassword());

        Authentication authenticate = authenticationManager.authenticate(authentication);
        SecurityContextHolder.getContext().setAuthentication(authenticate);

        User user = (User) authenticate.getPrincipal();
        return getLoginResponse(user);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String forgetPassword(ForgotPasswordRequest request) throws ValidationCustomException {
        validationUtil.validate(request);
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ValidationCustomException("Email not found", "email"));
        String token = jwtService.generateToken(user);
        user.setResetPasswordToken(token);
        userRepository.save(user);
        String subject = "Reset Password";
        String text = String.format("To reset your password, click the link below:\n http://localhost:3000/reset-password?token=%s", token);
        sendEmail(user.getEmail(), subject, text);

        return "Password reset link sent to your email";
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String resetPassword(String token, ResetPasswordRequest request) throws BadRequestException {
        validationUtil.validate(request);
        if (!jwtService.verifyJwtToken(token)) {
            throw new BadRequestException("Invalid or expired token");
        }

        User user = userRepository.findByResetPasswordToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid token"));
        String hashedPassword = passwordEncoder.encode(request.getPassword());
        user.setPassword(hashedPassword);
        user.setResetPasswordToken(null);
        return "Password reset successfully, please log in.";
    }

    private LoginResponse getLoginResponse(User user) {
        String token = jwtService.generateToken(user);
        return LoginResponse.builder()
                .username(user.getUsername())
                .token(token)
                .build();
    }

    private void sendEmail(String email, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}
