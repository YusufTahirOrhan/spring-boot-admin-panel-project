package com.optimaxx.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.optimaxx.management.domain.model.PasswordResetToken;
import com.optimaxx.management.domain.model.RefreshToken;
import com.optimaxx.management.domain.model.User;
import com.optimaxx.management.domain.model.UserRole;
import com.optimaxx.management.domain.repository.PasswordResetTokenRepository;
import com.optimaxx.management.domain.repository.RefreshTokenRepository;
import com.optimaxx.management.domain.repository.UserRepository;
import com.optimaxx.management.interfaces.rest.dto.AuthChangePasswordRequest;
import com.optimaxx.management.interfaces.rest.dto.AuthLoginRequest;
import com.optimaxx.management.interfaces.rest.dto.AuthLoginResponse;
import com.optimaxx.management.interfaces.rest.dto.AuthRefreshRequest;
import com.optimaxx.management.interfaces.rest.dto.ForgotPasswordResponse;
import com.optimaxx.management.interfaces.rest.dto.ResetPasswordRequest;
import com.optimaxx.management.security.AuthService;
import com.optimaxx.management.security.ForgotPasswordAttemptService;
import com.optimaxx.management.security.ForgotPasswordProtectionProperties;
import com.optimaxx.management.security.LoginAttemptService;
import com.optimaxx.management.security.LoginProtectionProperties;
import com.optimaxx.management.security.audit.SecurityAuditService;
import com.optimaxx.management.security.jwt.JwtProperties;
import com.optimaxx.management.security.jwt.JwtTokenService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class AuthServiceTest {

    @Test
    void shouldGenerateAccessAndRefreshTokenForValidCredentials() {
        JwtProperties jwtProperties = new JwtProperties("this-is-a-very-long-dev-secret-key-for-tests-123456", 60, 120, "test");
        JwtTokenService jwtTokenService = new JwtTokenService(jwtProperties);

        UserRepository userRepository = Mockito.mock(UserRepository.class);
        RefreshTokenRepository refreshTokenRepository = Mockito.mock(RefreshTokenRepository.class);
        PasswordResetTokenRepository passwordResetTokenRepository = Mockito.mock(PasswordResetTokenRepository.class);
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);

        User user = createActiveUser();

        when(userRepository.findByUsernameAndDeletedFalse("owner")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("owner123", "hashed")).thenReturn(true);

        AuthService authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                passwordResetTokenRepository,
                passwordEncoder,
                createLoginAttemptService(),
                createForgotPasswordAttemptService(),
                jwtTokenService,
                jwtProperties,
                Mockito.mock(SecurityAuditService.class)
        );

        AuthLoginResponse response = authService.login(new AuthLoginRequest("owner", "owner123"), "device-1", null, null);

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.role()).isEqualTo("OWNER");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void shouldRejectInvalidCredentialsWhenUserNotFound() {
        AuthService authService = createAuthServiceReturning(Optional.empty());

        assertThatThrownBy(() -> authService.login(new AuthLoginRequest("owner", "wrong"), "device-1", null, null))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void shouldLockUserAfterRepeatedFailedAttempts() {
        AuthService authService = createAuthServiceReturning(Optional.empty(), createLoginAttemptService(2, 1));

        assertThatThrownBy(() -> authService.login(new AuthLoginRequest("owner", "wrong1"), "device-1", null, null))
                .isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> authService.login(new AuthLoginRequest("owner", "wrong2"), "device-1", null, null))
                .isInstanceOf(BadCredentialsException.class);

        assertThatThrownBy(() -> authService.login(new AuthLoginRequest("owner", "wrong3"), "device-1", null, null))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldRejectInvalidCredentialsWhenUserInactive() {
        User inactiveUser = createActiveUser();
        inactiveUser.setActive(false);

        AuthService authService = createAuthServiceReturning(Optional.of(inactiveUser));

        assertThatThrownBy(() -> authService.login(new AuthLoginRequest("owner", "owner123"), "device-1", null, null))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void shouldRejectInvalidCredentialsWhenRequestContainsBlankValues() {
        JwtProperties jwtProperties = new JwtProperties("this-is-a-very-long-dev-secret-key-for-tests-123456", 60, 120, "test");
        JwtTokenService jwtTokenService = new JwtTokenService(jwtProperties);
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        RefreshTokenRepository refreshTokenRepository = Mockito.mock(RefreshTokenRepository.class);
        PasswordResetTokenRepository passwordResetTokenRepository = Mockito.mock(PasswordResetTokenRepository.class);
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);

        AuthService authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                passwordResetTokenRepository,
                passwordEncoder,
                createLoginAttemptService(),
                createForgotPasswordAttemptService(),
                jwtTokenService,
                jwtProperties,
                Mockito.mock(SecurityAuditService.class)
        );

        assertThatThrownBy(() -> authService.login(new AuthLoginRequest(" ", " "), "device-1", null, null))
                .isInstanceOf(BadCredentialsException.class);

        verify(userRepository, never()).findByUsernameAndDeletedFalse(anyString());
    }

    @Test
    void shouldRotateRefreshTokenWhenRefreshRequestIsValid() {
        JwtProperties jwtProperties = new JwtProperties("this-is-a-very-long-dev-secret-key-for-tests-123456", 60, 120, "test");
        JwtTokenService jwtTokenService = new JwtTokenService(jwtProperties);

        UserRepository userRepository = Mockito.mock(UserRepository.class);
        RefreshTokenRepository refreshTokenRepository = Mockito.mock(RefreshTokenRepository.class);
        PasswordResetTokenRepository passwordResetTokenRepository = Mockito.mock(PasswordResetTokenRepository.class);
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);

        User user = createActiveUser();
        RefreshToken existingToken = new RefreshToken();
        existingToken.setUser(user);
        existingToken.setRevoked(false);
        existingToken.setDeviceId("device-1");
        existingToken.setExpiresAt(Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByTokenHashAndRevokedFalseAndExpiresAtAfter(anyString(), any(Instant.class)))
                .thenReturn(Optional.of(existingToken));

        AuthService authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                passwordResetTokenRepository,
                passwordEncoder,
                createLoginAttemptService(),
                createForgotPasswordAttemptService(),
                jwtTokenService,
                jwtProperties,
                Mockito.mock(SecurityAuditService.class)
        );

        AuthLoginResponse response = authService.refresh(new AuthRefreshRequest("raw-refresh-token"), "device-1", null, null);

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(existingToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void shouldChangePasswordWhenCurrentPasswordMatches() {
        JwtProperties jwtProperties = new JwtProperties("this-is-a-very-long-dev-secret-key-for-tests-123456", 60, 120, "test");
        JwtTokenService jwtTokenService = new JwtTokenService(jwtProperties);

        UserRepository userRepository = Mockito.mock(UserRepository.class);
        RefreshTokenRepository refreshTokenRepository = Mockito.mock(RefreshTokenRepository.class);
        PasswordResetTokenRepository passwordResetTokenRepository = Mockito.mock(PasswordResetTokenRepository.class);
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);

        User user = createActiveUser();
        when(userRepository.findByUsernameAndDeletedFalse("owner")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("owner123", "hashed")).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("new-hash");

        AuthService authService = new AuthService(userRepository, refreshTokenRepository, passwordResetTokenRepository, passwordEncoder, createLoginAttemptService(), createForgotPasswordAttemptService(), jwtTokenService, jwtProperties, Mockito.mock(SecurityAuditService.class));

        authService.changePassword("owner", new AuthChangePasswordRequest("owner123", "newPassword123"));

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
    }

    @Test
    void shouldListDeviceSessionsForUser() {
        JwtProperties jwtProperties = new JwtProperties("this-is-a-very-long-dev-secret-key-for-tests-123456", 60, 120, "test");
        JwtTokenService jwtTokenService = new JwtTokenService(jwtProperties);

        UserRepository userRepository = Mockito.mock(UserRepository.class);
        RefreshTokenRepository refreshTokenRepository = Mockito.mock(RefreshTokenRepository.class);
        PasswordResetTokenRepository passwordResetTokenRepository = Mockito.mock(PasswordResetTokenRepository.class);
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);

        User user = createActiveUser();
        when(userRepository.findByUsernameAndDeletedFalse("owner")).thenReturn(Optional.of(user));

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setDeviceId("device-1");
        token.setExpiresAt(Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByUserAndRevokedFalseAndExpiresAtAfter(any(User.class), any(Instant.class)))
                .thenReturn(java.util.List.of(token));

        AuthService authService = new AuthService(userRepository, refreshTokenRepository, passwordResetTokenRepository, passwordEncoder, createLoginAttemptService(), createForgotPasswordAttemptService(), jwtTokenService, jwtProperties, Mockito.mock(SecurityAuditService.class));

        assertThat(authService.listDeviceSessions("owner", "device-1")).hasSize(1);
    }

    @Test
    void shouldLogoutSpecificDevice() {
        JwtProperties jwtProperties = new JwtProperties("this-is-a-very-long-dev-secret-key-for-tests-123456", 60, 120, "test");
        JwtTokenService jwtTokenService = new JwtTokenService(jwtProperties);

        UserRepository userRepository = Mockito.mock(UserRepository.class);
        RefreshTokenRepository refreshTokenRepository = Mockito.mock(RefreshTokenRepository.class);
        PasswordResetTokenRepository passwordResetTokenRepository = Mockito.mock(PasswordResetTokenRepository.class);
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);

        User user = createActiveUser();
        when(userRepository.findByUsernameAndDeletedFalse("owner")).thenReturn(Optional.of(user));

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setDeviceId("device-1");
        token.setRevoked(false);
        token.setExpiresAt(Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByUserAndDeviceIdAndRevokedFalseAndExpiresAtAfter(any(User.class), anyString(), any(Instant.class)))
                .thenReturn(java.util.List.of(token));

        AuthService authService = new AuthService(userRepository, refreshTokenRepository, passwordResetTokenRepository, passwordEncoder, createLoginAttemptService(), createForgotPasswordAttemptService(), jwtTokenService, jwtProperties, Mockito.mock(SecurityAuditService.class));

        authService.logoutDevice("owner", "device-1");

        assertThat(token.isRevoked()).isTrue();
    }

    @Test
    void shouldIssuePasswordResetTokenWhenEmailExists() {
        JwtProperties jwtProperties = new JwtProperties("this-is-a-very-long-dev-secret-key-for-tests-123456", 60, 120, "test");
        JwtTokenService jwtTokenService = new JwtTokenService(jwtProperties);

        UserRepository userRepository = Mockito.mock(UserRepository.class);
        RefreshTokenRepository refreshTokenRepository = Mockito.mock(RefreshTokenRepository.class);
        PasswordResetTokenRepository passwordResetTokenRepository = Mockito.mock(PasswordResetTokenRepository.class);
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);

        User user = createActiveUser();
        user.setEmail("owner@optimaxx.local");
        when(userRepository.findByEmailAndDeletedFalse("owner@optimaxx.local")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUserAndUsedFalseAndExpiresAtAfter(any(User.class), any(Instant.class)))
                .thenReturn(java.util.List.of());

        AuthService authService = new AuthService(userRepository, refreshTokenRepository, passwordResetTokenRepository, passwordEncoder, createLoginAttemptService(), createForgotPasswordAttemptService(), jwtTokenService, jwtProperties, Mockito.mock(SecurityAuditService.class));

        ForgotPasswordResponse response = authService.forgotPassword("owner@optimaxx.local");

        assertThat(response.message()).isNotBlank();
        assertThat(response.expiresInMinutes()).isEqualTo(15);
        verify(passwordResetTokenRepository).save(any());
    }

    @Test
    void shouldResetPasswordAndRevokeSessions() {
        JwtProperties jwtProperties = new JwtProperties("this-is-a-very-long-dev-secret-key-for-tests-123456", 60, 120, "test");
        JwtTokenService jwtTokenService = new JwtTokenService(jwtProperties);

        UserRepository userRepository = Mockito.mock(UserRepository.class);
        RefreshTokenRepository refreshTokenRepository = Mockito.mock(RefreshTokenRepository.class);
        PasswordResetTokenRepository passwordResetTokenRepository = Mockito.mock(PasswordResetTokenRepository.class);
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);

        User user = createActiveUser();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setUsed(false);
        resetToken.setExpiresAt(Instant.now().plusSeconds(3600));

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setRevoked(false);
        refreshToken.setExpiresAt(Instant.now().plusSeconds(3600));

        when(passwordResetTokenRepository.findByTokenHashAndUsedFalseAndExpiresAtAfter(anyString(), any(Instant.class)))
                .thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("newPassword123")).thenReturn("new-hash");
        when(refreshTokenRepository.findByUserAndRevokedFalseAndExpiresAtAfter(any(User.class), any(Instant.class)))
                .thenReturn(java.util.List.of(refreshToken));

        AuthService authService = new AuthService(userRepository, refreshTokenRepository, passwordResetTokenRepository, passwordEncoder, createLoginAttemptService(), createForgotPasswordAttemptService(), jwtTokenService, jwtProperties, Mockito.mock(SecurityAuditService.class));

        authService.resetPassword(new ResetPasswordRequest("raw-token", "newPassword123"));

        assertThat(resetToken.isUsed()).isTrue();
        assertThat(refreshToken.isRevoked()).isTrue();
        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
    }

    @Test
    void shouldRejectPasswordChangeWhenCurrentPasswordIsInvalid() {
        JwtProperties jwtProperties = new JwtProperties("this-is-a-very-long-dev-secret-key-for-tests-123456", 60, 120, "test");
        JwtTokenService jwtTokenService = new JwtTokenService(jwtProperties);

        UserRepository userRepository = Mockito.mock(UserRepository.class);
        RefreshTokenRepository refreshTokenRepository = Mockito.mock(RefreshTokenRepository.class);
        PasswordResetTokenRepository passwordResetTokenRepository = Mockito.mock(PasswordResetTokenRepository.class);
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);

        User user = createActiveUser();
        when(userRepository.findByUsernameAndDeletedFalse("owner")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        AuthService authService = new AuthService(userRepository, refreshTokenRepository, passwordResetTokenRepository, passwordEncoder, createLoginAttemptService(), createForgotPasswordAttemptService(), jwtTokenService, jwtProperties, Mockito.mock(SecurityAuditService.class));

        assertThatThrownBy(() -> authService.changePassword("owner", new AuthChangePasswordRequest("wrong", "newPassword123")))
                .isInstanceOf(BadCredentialsException.class);
    }

    private AuthService createAuthServiceReturning(Optional<User> userOptional) {
        return createAuthServiceReturning(userOptional, createLoginAttemptService());
    }

    private AuthService createAuthServiceReturning(Optional<User> userOptional, LoginAttemptService loginAttemptService) {
        JwtProperties jwtProperties = new JwtProperties("this-is-a-very-long-dev-secret-key-for-tests-123456", 60, 120, "test");
        JwtTokenService jwtTokenService = new JwtTokenService(jwtProperties);

        UserRepository userRepository = Mockito.mock(UserRepository.class);
        RefreshTokenRepository refreshTokenRepository = Mockito.mock(RefreshTokenRepository.class);
        PasswordResetTokenRepository passwordResetTokenRepository = Mockito.mock(PasswordResetTokenRepository.class);
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);

        when(userRepository.findByUsernameAndDeletedFalse(anyString())).thenReturn(userOptional);

        return new AuthService(
                userRepository,
                refreshTokenRepository,
                passwordResetTokenRepository,
                passwordEncoder,
                loginAttemptService,
                createForgotPasswordAttemptService(),
                jwtTokenService,
                jwtProperties,
                Mockito.mock(SecurityAuditService.class)
        );
    }

    private LoginAttemptService createLoginAttemptService() {
        return createLoginAttemptService(5, 15);
    }

    private LoginAttemptService createLoginAttemptService(int maxFailures, long lockMinutes) {
        @SuppressWarnings("unchecked")
        ObjectProvider<org.springframework.data.redis.core.StringRedisTemplate> redisProvider = Mockito.mock(ObjectProvider.class);
        when(redisProvider.getIfAvailable()).thenReturn(null);
        return new LoginAttemptService(new LoginProtectionProperties(maxFailures, lockMinutes), redisProvider);
    }

    private ForgotPasswordAttemptService createForgotPasswordAttemptService() {
        @SuppressWarnings("unchecked")
        ObjectProvider<org.springframework.data.redis.core.StringRedisTemplate> redisProvider = Mockito.mock(ObjectProvider.class);
        when(redisProvider.getIfAvailable()).thenReturn(null);
        return new ForgotPasswordAttemptService(new ForgotPasswordProtectionProperties(2, 5), redisProvider);
    }

    private User createActiveUser() {
        User user = new User();
        user.setUsername("owner");
        user.setPasswordHash("hashed");
        user.setRole(UserRole.OWNER);
        user.setActive(true);
        user.setDeleted(false);
        return user;
    }
}
