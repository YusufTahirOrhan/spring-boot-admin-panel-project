package com.optimaxx.management.security;

import com.optimaxx.management.domain.model.User;
import com.optimaxx.management.domain.repository.UserRepository;
import com.optimaxx.management.interfaces.rest.dto.AuthLoginRequest;
import com.optimaxx.management.interfaces.rest.dto.AuthLoginResponse;
import com.optimaxx.management.security.jwt.JwtProperties;
import com.optimaxx.management.security.jwt.JwtTokenService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid credentials";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenService jwtTokenService,
                       JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.jwtProperties = jwtProperties;
    }

    public AuthLoginResponse login(AuthLoginRequest request) {
        if (request == null || isBlank(request.username()) || isBlank(request.password())) {
            throw new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }

        String normalizedUsername = request.username().trim();
        User user = userRepository.findByUsernameAndDeletedFalse(normalizedUsername)
                .orElseThrow(() -> new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE));

        if (!user.isActive() || user.getRole() == null || isBlank(user.getPasswordHash())
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }

        String role = user.getRole().name();
        String token = jwtTokenService.generateAccessToken(user.getUsername(), role);
        return new AuthLoginResponse(token, "Bearer", jwtProperties.accessTokenMinutes(), role);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
