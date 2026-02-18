package com.optimaxx.management.security;

import com.optimaxx.management.domain.model.RefreshToken;
import com.optimaxx.management.domain.model.User;
import com.optimaxx.management.domain.repository.RefreshTokenRepository;
import com.optimaxx.management.domain.repository.UserRepository;
import com.optimaxx.management.interfaces.rest.dto.AuthChangePasswordRequest;
import com.optimaxx.management.interfaces.rest.dto.AuthLoginRequest;
import com.optimaxx.management.interfaces.rest.dto.AuthLoginResponse;
import com.optimaxx.management.interfaces.rest.dto.AuthRefreshRequest;
import com.optimaxx.management.security.jwt.JwtProperties;
import com.optimaxx.management.security.jwt.JwtTokenService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid credentials";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       LoginAttemptService loginAttemptService,
                       JwtTokenService jwtTokenService,
                       JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
        this.jwtTokenService = jwtTokenService;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public AuthLoginResponse login(AuthLoginRequest request) {
        if (request == null || isBlank(request.username()) || isBlank(request.password())) {
            throw new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }

        String normalizedUsername = request.username().trim();
        loginAttemptService.checkBlocked(normalizedUsername);

        User user = userRepository.findByUsernameAndDeletedFalse(normalizedUsername)
                .orElseThrow(() -> {
                    loginAttemptService.onFailedAttempt(normalizedUsername);
                    return new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE);
                });

        if (!isUserEligible(user) || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginAttemptService.onFailedAttempt(normalizedUsername);
            throw new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }

        loginAttemptService.onSuccessfulAttempt(normalizedUsername);
        return issueTokenPair(user);
    }

    @Transactional
    public AuthLoginResponse refresh(AuthRefreshRequest request) {
        if (request == null || isBlank(request.refreshToken())) {
            throw new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }

        Instant now = Instant.now();
        String currentTokenHash = hashToken(request.refreshToken());

        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHashAndRevokedFalseAndExpiresAtAfter(currentTokenHash, now)
                .orElseThrow(() -> new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE));

        User user = refreshToken.getUser();
        if (!isUserEligible(user)) {
            throw new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }

        refreshToken.setRevoked(true);
        refreshToken.setRevokedAt(now);

        return issueTokenPair(user);
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        if (isBlank(refreshTokenValue)) {
            return;
        }

        refreshTokenRepository
                .findByTokenHashAndRevokedFalseAndExpiresAtAfter(hashToken(refreshTokenValue), Instant.now())
                .ifPresent(token -> {
                    token.setRevoked(true);
                    token.setRevokedAt(Instant.now());
                });
    }

    @Transactional
    public void changePassword(String username, AuthChangePasswordRequest request) {
        if (isBlank(username) || request == null || isBlank(request.currentPassword()) || isBlank(request.newPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password and new password are required");
        }

        if (request.newPassword().length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be at least 8 characters");
        }

        User user = userRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    private AuthLoginResponse issueTokenPair(User user) {
        String role = user.getRole().name();
        String accessToken = jwtTokenService.generateAccessToken(user.getUsername(), role);
        String refreshToken = generateOpaqueToken();

        RefreshToken refreshTokenEntity = new RefreshToken();
        refreshTokenEntity.setUser(user);
        refreshTokenEntity.setTokenHash(hashToken(refreshToken));
        refreshTokenEntity.setExpiresAt(Instant.now().plus(jwtProperties.refreshTokenMinutes(), ChronoUnit.MINUTES));
        refreshTokenEntity.setRevoked(false);
        refreshTokenRepository.save(refreshTokenEntity);

        return new AuthLoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtProperties.accessTokenMinutes(),
                role
        );
    }

    private boolean isUserEligible(User user) {
        return user.isActive() && !user.isDeleted() && user.getRole() != null && !isBlank(user.getPasswordHash());
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
