package com.optimaxx.management.security;

import com.optimaxx.management.domain.model.RefreshToken;
import com.optimaxx.management.domain.model.User;
import com.optimaxx.management.domain.repository.RefreshTokenRepository;
import com.optimaxx.management.domain.repository.UserRepository;
import com.optimaxx.management.interfaces.rest.dto.AuthChangePasswordRequest;
import com.optimaxx.management.interfaces.rest.dto.AuthLoginRequest;
import com.optimaxx.management.interfaces.rest.dto.AuthLoginResponse;
import com.optimaxx.management.interfaces.rest.dto.AuthRefreshRequest;
import com.optimaxx.management.interfaces.rest.dto.DeviceSessionResponse;
import com.optimaxx.management.security.audit.AuditEventType;
import com.optimaxx.management.security.audit.SecurityAuditService;
import com.optimaxx.management.security.jwt.JwtProperties;
import com.optimaxx.management.security.jwt.JwtTokenService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid credentials";
    private static final String UNKNOWN_DEVICE = "unknown-device";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;
    private final SecurityAuditService securityAuditService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       LoginAttemptService loginAttemptService,
                       JwtTokenService jwtTokenService,
                       JwtProperties jwtProperties,
                       SecurityAuditService securityAuditService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
        this.jwtTokenService = jwtTokenService;
        this.jwtProperties = jwtProperties;
        this.securityAuditService = securityAuditService;
    }

    @Transactional
    public AuthLoginResponse login(AuthLoginRequest request, String deviceId, String userAgent, String ipAddress) {
        if (request == null || isBlank(request.username()) || isBlank(request.password())) {
            throw new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }

        String normalizedUsername = request.username().trim();
        try {
            loginAttemptService.checkBlocked(normalizedUsername);
        } catch (ResponseStatusException ex) {
            securityAuditService.log(AuditEventType.LOGIN_BLOCKED, null, "AUTH", normalizedUsername, "{\"status\":\"blocked\"}");
            throw ex;
        }

        User user = userRepository.findByUsernameAndDeletedFalse(normalizedUsername)
                .orElseThrow(() -> {
                    loginAttemptService.onFailedAttempt(normalizedUsername);
                    securityAuditService.log(AuditEventType.LOGIN_FAILED, null, "AUTH", normalizedUsername, "{\"reason\":\"user-not-found\"}");
                    return new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE);
                });

        if (!isUserEligible(user) || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginAttemptService.onFailedAttempt(normalizedUsername);
            securityAuditService.log(AuditEventType.LOGIN_FAILED, user, "AUTH", normalizedUsername, "{\"reason\":\"invalid-credentials\"}");
            throw new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }

        loginAttemptService.onSuccessfulAttempt(normalizedUsername);
        securityAuditService.log(AuditEventType.LOGIN_SUCCESS, user, "AUTH", normalizedUsername, "{\"status\":\"success\"}");
        return issueTokenPair(user, deviceId, userAgent, ipAddress);
    }

    @Transactional
    public AuthLoginResponse refresh(AuthRefreshRequest request, String deviceId, String userAgent, String ipAddress) {
        if (request == null || isBlank(request.refreshToken())) {
            throw new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }

        Instant now = Instant.now();
        String currentTokenHash = hashToken(request.refreshToken());

        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHashAndRevokedFalseAndExpiresAtAfter(currentTokenHash, now)
                .orElseThrow(() -> new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE));

        User user = refreshToken.getUser();
        if (!isUserEligible(user) || !normalizeDeviceId(deviceId).equals(refreshToken.getDeviceId())) {
            throw new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }

        refreshToken.setRevoked(true);
        refreshToken.setRevokedAt(now);
        securityAuditService.log(AuditEventType.TOKEN_REFRESHED, user, "AUTH", user.getUsername(), "{\"status\":\"rotated\"}");

        return issueTokenPair(user, deviceId, userAgent, ipAddress);
    }

    @Transactional
    public void logout(String refreshTokenValue, String deviceId) {
        if (isBlank(refreshTokenValue)) {
            return;
        }

        refreshTokenRepository
                .findByTokenHashAndRevokedFalseAndExpiresAtAfter(hashToken(refreshTokenValue), Instant.now())
                .ifPresent(token -> {
                    if (!normalizeDeviceId(deviceId).equals(token.getDeviceId())) {
                        return;
                    }
                    token.setRevoked(true);
                    token.setRevokedAt(Instant.now());
                    securityAuditService.log(
                            AuditEventType.LOGOUT,
                            token.getUser(),
                            "AUTH",
                            token.getUser().getUsername(),
                            "{\"status\":\"revoked-this-device\"}"
                    );
                });
    }

    @Transactional
    public void logoutAllDevices(String username) {
        if (isBlank(username)) {
            return;
        }

        User user = userRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<RefreshToken> activeTokens = refreshTokenRepository
                .findByUserAndRevokedFalseAndExpiresAtAfter(user, Instant.now());

        for (RefreshToken token : activeTokens) {
            token.setRevoked(true);
            token.setRevokedAt(Instant.now());
        }

        securityAuditService.log(
                AuditEventType.LOGOUT,
                user,
                "AUTH",
                user.getUsername(),
                "{\"status\":\"revoked-all-devices\",\"count\":" + activeTokens.size() + "}"
        );
    }

    @Transactional(readOnly = true)
    public List<DeviceSessionResponse> listDeviceSessions(String username, String currentDeviceId) {
        if (isBlank(username)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is required");
        }

        User user = userRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String normalizedCurrentDeviceId = normalizeDeviceId(currentDeviceId);

        return refreshTokenRepository.findByUserAndRevokedFalseAndExpiresAtAfter(user, Instant.now())
                .stream()
                .sorted(Comparator.comparing(RefreshToken::getCreatedAt).reversed())
                .map(token -> new DeviceSessionResponse(
                        token.getDeviceId(),
                        token.getUserAgent(),
                        token.getIpAddress(),
                        token.getCreatedAt(),
                        token.getExpiresAt(),
                        token.getDeviceId().equals(normalizedCurrentDeviceId)
                ))
                .toList();
    }

    @Transactional
    public void logoutDevice(String username, String targetDeviceId) {
        if (isBlank(username) || isBlank(targetDeviceId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username and target device are required");
        }

        User user = userRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<RefreshToken> deviceTokens = refreshTokenRepository
                .findByUserAndDeviceIdAndRevokedFalseAndExpiresAtAfter(user, targetDeviceId.trim(), Instant.now());

        for (RefreshToken token : deviceTokens) {
            token.setRevoked(true);
            token.setRevokedAt(Instant.now());
        }

        securityAuditService.log(
                AuditEventType.DEVICE_SESSION_REVOKED,
                user,
                "AUTH",
                user.getUsername(),
                "{\"deviceId\":\"" + targetDeviceId.trim() + "\",\"count\":" + deviceTokens.size() + "}"
        );
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
        securityAuditService.log(AuditEventType.PASSWORD_CHANGED, user, "AUTH", user.getUsername(), "{\"status\":\"updated\"}");
    }

    private AuthLoginResponse issueTokenPair(User user, String deviceId, String userAgent, String ipAddress) {
        String role = user.getRole().name();
        String accessToken = jwtTokenService.generateAccessToken(user.getUsername(), role);
        String refreshToken = generateOpaqueToken();

        RefreshToken refreshTokenEntity = new RefreshToken();
        refreshTokenEntity.setUser(user);
        refreshTokenEntity.setDeviceId(normalizeDeviceId(deviceId));
        refreshTokenEntity.setUserAgent(isBlank(userAgent) ? null : userAgent);
        refreshTokenEntity.setIpAddress(isBlank(ipAddress) ? null : ipAddress);
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

    private String normalizeDeviceId(String deviceId) {
        if (isBlank(deviceId)) {
            return UNKNOWN_DEVICE;
        }
        return deviceId.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
