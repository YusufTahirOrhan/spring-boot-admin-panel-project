package com.optimaxx.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.optimaxx.management.domain.model.RefreshToken;
import com.optimaxx.management.domain.model.User;
import com.optimaxx.management.domain.model.UserRole;
import com.optimaxx.management.domain.repository.RefreshTokenRepository;
import com.optimaxx.management.domain.repository.UserRepository;
import com.optimaxx.management.interfaces.rest.dto.AuthLoginRequest;
import com.optimaxx.management.interfaces.rest.dto.AuthLoginResponse;
import com.optimaxx.management.interfaces.rest.dto.AuthRefreshRequest;
import com.optimaxx.management.security.AuthService;
import com.optimaxx.management.security.jwt.JwtProperties;
import com.optimaxx.management.security.jwt.JwtTokenService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

    @Test
    void shouldGenerateAccessAndRefreshTokenForValidCredentials() {
        JwtProperties jwtProperties = new JwtProperties("this-is-a-very-long-dev-secret-key-for-tests-123456", 60, 120, "test");
        JwtTokenService jwtTokenService = new JwtTokenService(jwtProperties);

        UserRepository userRepository = Mockito.mock(UserRepository.class);
        RefreshTokenRepository refreshTokenRepository = Mockito.mock(RefreshTokenRepository.class);
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);

        User user = createActiveUser();

        when(userRepository.findByUsernameAndDeletedFalse("owner")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("owner123", "hashed")).thenReturn(true);

        AuthService authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtTokenService,
                jwtProperties
        );

        AuthLoginResponse response = authService.login(new AuthLoginRequest("owner", "owner123"));

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.role()).isEqualTo("OWNER");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void shouldRejectInvalidCredentialsWhenUserNotFound() {
        AuthService authService = createAuthServiceReturning(Optional.empty());

        assertThatThrownBy(() -> authService.login(new AuthLoginRequest("owner", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void shouldRejectInvalidCredentialsWhenUserInactive() {
        User inactiveUser = createActiveUser();
        inactiveUser.setActive(false);

        AuthService authService = createAuthServiceReturning(Optional.of(inactiveUser));

        assertThatThrownBy(() -> authService.login(new AuthLoginRequest("owner", "owner123")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void shouldRejectInvalidCredentialsWhenRequestContainsBlankValues() {
        JwtProperties jwtProperties = new JwtProperties("this-is-a-very-long-dev-secret-key-for-tests-123456", 60, 120, "test");
        JwtTokenService jwtTokenService = new JwtTokenService(jwtProperties);
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        RefreshTokenRepository refreshTokenRepository = Mockito.mock(RefreshTokenRepository.class);
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);

        AuthService authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtTokenService,
                jwtProperties
        );

        assertThatThrownBy(() -> authService.login(new AuthLoginRequest(" ", " ")))
                .isInstanceOf(BadCredentialsException.class);

        verify(userRepository, never()).findByUsernameAndDeletedFalse(anyString());
    }

    @Test
    void shouldRotateRefreshTokenWhenRefreshRequestIsValid() {
        JwtProperties jwtProperties = new JwtProperties("this-is-a-very-long-dev-secret-key-for-tests-123456", 60, 120, "test");
        JwtTokenService jwtTokenService = new JwtTokenService(jwtProperties);

        UserRepository userRepository = Mockito.mock(UserRepository.class);
        RefreshTokenRepository refreshTokenRepository = Mockito.mock(RefreshTokenRepository.class);
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);

        User user = createActiveUser();
        RefreshToken existingToken = new RefreshToken();
        existingToken.setUser(user);
        existingToken.setRevoked(false);
        existingToken.setExpiresAt(Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByTokenHashAndRevokedFalseAndExpiresAtAfter(anyString(), any(Instant.class)))
                .thenReturn(Optional.of(existingToken));

        AuthService authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtTokenService,
                jwtProperties
        );

        AuthLoginResponse response = authService.refresh(new AuthRefreshRequest("raw-refresh-token"));

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(existingToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    private AuthService createAuthServiceReturning(Optional<User> userOptional) {
        JwtProperties jwtProperties = new JwtProperties("this-is-a-very-long-dev-secret-key-for-tests-123456", 60, 120, "test");
        JwtTokenService jwtTokenService = new JwtTokenService(jwtProperties);

        UserRepository userRepository = Mockito.mock(UserRepository.class);
        RefreshTokenRepository refreshTokenRepository = Mockito.mock(RefreshTokenRepository.class);
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);

        when(userRepository.findByUsernameAndDeletedFalse(anyString())).thenReturn(userOptional);

        return new AuthService(
                userRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtTokenService,
                jwtProperties
        );
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
