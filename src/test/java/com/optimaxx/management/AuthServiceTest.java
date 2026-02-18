package com.optimaxx.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.optimaxx.management.domain.model.User;
import com.optimaxx.management.domain.model.UserRole;
import com.optimaxx.management.domain.repository.UserRepository;
import com.optimaxx.management.interfaces.rest.dto.AuthLoginRequest;
import com.optimaxx.management.interfaces.rest.dto.AuthLoginResponse;
import com.optimaxx.management.security.AuthService;
import com.optimaxx.management.security.jwt.JwtProperties;
import com.optimaxx.management.security.jwt.JwtTokenService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

    @Test
    void shouldGenerateAccessTokenForValidCredentials() {
        JwtProperties jwtProperties = new JwtProperties("this-is-a-very-long-dev-secret-key-for-tests-123456", 60, "test");
        JwtTokenService jwtTokenService = new JwtTokenService(jwtProperties);

        UserRepository userRepository = Mockito.mock(UserRepository.class);
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);

        User user = new User();
        user.setUsername("owner");
        user.setPasswordHash("hashed");
        user.setRole(UserRole.OWNER);
        user.setActive(true);

        when(userRepository.findByUsernameAndDeletedFalse("owner")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("owner123", "hashed")).thenReturn(true);

        AuthService authService = new AuthService(userRepository, passwordEncoder, jwtTokenService, jwtProperties);

        AuthLoginResponse response = authService.login(new AuthLoginRequest("owner", "owner123"));

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.role()).isEqualTo("OWNER");
    }

    @Test
    void shouldRejectInvalidCredentials() {
        JwtProperties jwtProperties = new JwtProperties("this-is-a-very-long-dev-secret-key-for-tests-123456", 60, "test");
        JwtTokenService jwtTokenService = new JwtTokenService(jwtProperties);

        UserRepository userRepository = Mockito.mock(UserRepository.class);
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);

        when(userRepository.findByUsernameAndDeletedFalse(anyString())).thenReturn(Optional.empty());

        AuthService authService = new AuthService(userRepository, passwordEncoder, jwtTokenService, jwtProperties);

        assertThatThrownBy(() -> authService.login(new AuthLoginRequest("owner", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
