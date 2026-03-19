package com.esign.service.impl;

import com.esign.constant.StatusMessage;
import com.esign.entities.user.ForgotPasswordRequest;
import com.esign.entities.user.LoginRequest;
import com.esign.entities.user.LoginResponse;
import com.esign.entities.user.ResetPasswordRequest;
import com.esign.exception.BadRequestException;
import com.esign.exception.NotFoundException;
import com.esign.exception.ValidationCustomException;
import com.esign.model.*;
import com.esign.repository.*;
import com.esign.model.*;
import com.esign.repository.*;
import com.esign.service.AuthService;
import com.esign.service.EmailService;
import com.esign.service.JwtService;
import com.esign.validation.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final ValidationUtil validationUtil;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final EmailService emailService;

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
        emailService.sendEmail(user.getEmail(), subject, text);

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

    @Transactional(rollbackFor = Exception.class)
    @Override
    public User getAuthenticatedUser() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByIdAndIsEnableTrue(userId)
                .orElseThrow(() -> new NotFoundException(StatusMessage.USER_NOT_FOUND));
    }

    private LoginResponse getLoginResponse(User user) {
        String token = jwtService.generateToken(user);
        return LoginResponse.builder()
                .username(user.getUsername())
                .token(token)
                .build();
    }

}
