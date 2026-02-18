package com.optimaxx.management.security;

import com.optimaxx.management.interfaces.rest.dto.AuthLoginRequest;
import com.optimaxx.management.interfaces.rest.dto.AuthLoginResponse;
import com.optimaxx.management.security.jwt.JwtProperties;
import com.optimaxx.management.security.jwt.JwtTokenService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final String DEMO_USERNAME = "owner";
    private static final String DEMO_PASSWORD = "owner123";
    private static final String DEMO_ROLE = "OWNER";

    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;

    public AuthService(JwtTokenService jwtTokenService, JwtProperties jwtProperties) {
        this.jwtTokenService = jwtTokenService;
        this.jwtProperties = jwtProperties;
    }

    public AuthLoginResponse login(AuthLoginRequest request) {
        if (!DEMO_USERNAME.equals(request.username()) || !DEMO_PASSWORD.equals(request.password())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String token = jwtTokenService.generateAccessToken(DEMO_USERNAME, DEMO_ROLE);
        return new AuthLoginResponse(token, "Bearer", jwtProperties.accessTokenMinutes(), DEMO_ROLE);
    }
}
