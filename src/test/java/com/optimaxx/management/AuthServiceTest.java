package com.optimaxx.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.optimaxx.management.interfaces.rest.dto.AuthLoginRequest;
import com.optimaxx.management.interfaces.rest.dto.AuthLoginResponse;
import com.optimaxx.management.security.AuthService;
import com.optimaxx.management.security.jwt.JwtProperties;
import com.optimaxx.management.security.jwt.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

class AuthServiceTest {

    @Test
    void shouldGenerateAccessTokenForValidDemoCredentials() {
        JwtProperties jwtProperties = new JwtProperties("this-is-a-very-long-dev-secret-key-for-tests-123456", 60, "test");
        AuthService authService = new AuthService(new JwtTokenService(jwtProperties), jwtProperties);

        AuthLoginResponse response = authService.login(new AuthLoginRequest("owner", "owner123"));

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.role()).isEqualTo("OWNER");
    }

    @Test
    void shouldRejectInvalidCredentials() {
        JwtProperties jwtProperties = new JwtProperties("this-is-a-very-long-dev-secret-key-for-tests-123456", 60, "test");
        AuthService authService = new AuthService(new JwtTokenService(jwtProperties), jwtProperties);

        assertThatThrownBy(() -> authService.login(new AuthLoginRequest("owner", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
