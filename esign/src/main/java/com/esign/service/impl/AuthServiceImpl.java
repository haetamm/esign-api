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
import org.springframework.beans.factory.annotation.Value;
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
    private final TokenRepository tokenRepository;

    @Value("${esign_api.frontend.url}")
    private String frontendUrl;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public LoginResponse login(LoginRequest request) {
        validationUtil.validate(request);
        Authentication authentication = new UsernamePasswordAuthenticationToken(request.getUsername(),
                request.getPassword());

        Authentication authenticate = authenticationManager.authenticate(authentication);
        SecurityContextHolder.getContext().setAuthentication(authenticate);

        User user = (User) authenticate.getPrincipal();
        return generateAndSaveTokens(user);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public LoginResponse refreshToken(String refreshToken) throws BadRequestException {
        Token savedToken = tokenRepository.findByRefreshTokenAndIsRevokedFalse(refreshToken)
                .orElseThrow(() -> new NotFoundException(StatusMessage.INVALID_OR_EXPIRED_REFRESH_TOKEN));

        User user = savedToken.getUser();

        if (jwtService.isTokenInvalid(refreshToken)) {
            savedToken.setIsRevoked(true);
            tokenRepository.save(savedToken);
            throw new BadRequestException(StatusMessage.REFRESH_TOKEN_EXPIRED);
        }

        savedToken.setIsRevoked(true);
        tokenRepository.save(savedToken);

        return generateAndSaveTokens(user);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void logout(String bearerToken) {
        // Ekstrak raw token dari header "Bearer <token>"
        String rawToken = jwtService.parseToken(bearerToken);

        Token savedToken = tokenRepository.findByAccessTokenAndIsRevokedFalse(rawToken)
                .orElseThrow(() -> new NotFoundException(StatusMessage.TOKEN_NOT_FOUND));

        // Revoke access token
        savedToken.setIsRevoked(true);
        tokenRepository.save(savedToken);

        SecurityContextHolder.clearContext();
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
        String text = String.format(
                "To reset your password, click the link below:\n%s/reset-password?token=%s",
                frontendUrl,
                token
        );
        emailService.sendEmailAfterCommit(user.getEmail(), subject, text);

        return "Password reset link sent to your email";
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String resetPassword(String token, ResetPasswordRequest request) throws BadRequestException {
        validationUtil.validate(request);
        if (jwtService.isTokenInvalid(token)) {
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

    private LoginResponse generateAndSaveTokens(User user) {
        tokenRepository.revokeAllActiveTokenByUserId(user.getId());

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        Token token = Token.builder()
                .user(user)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
        tokenRepository.save(token);

        return LoginResponse.builder()
                .username(user.getUsername())
                .token(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

}
