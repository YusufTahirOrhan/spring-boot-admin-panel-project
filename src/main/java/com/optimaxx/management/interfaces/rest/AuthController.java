package com.optimaxx.management.interfaces.rest;

import com.optimaxx.management.interfaces.rest.dto.AuthChangePasswordRequest;
import com.optimaxx.management.interfaces.rest.dto.AuthLoginRequest;
import com.optimaxx.management.interfaces.rest.dto.AuthLoginResponse;
import com.optimaxx.management.interfaces.rest.dto.AuthLogoutRequest;
import com.optimaxx.management.interfaces.rest.dto.AuthRefreshRequest;
import com.optimaxx.management.security.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthLoginResponse login(@RequestBody AuthLoginRequest request,
                                   @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
                                   @RequestHeader(value = "User-Agent", required = false) String userAgent,
                                   @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor) {
        return authService.login(request, deviceId, userAgent, forwardedFor);
    }

    @PostMapping("/refresh")
    public AuthLoginResponse refresh(@RequestBody AuthRefreshRequest request,
                                     @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
                                     @RequestHeader(value = "User-Agent", required = false) String userAgent,
                                     @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor) {
        return authService.refresh(request, deviceId, userAgent, forwardedFor);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody AuthLogoutRequest request,
                                       @RequestHeader(value = "X-Device-Id", required = false) String deviceId) {
        authService.logout(request == null ? null : request.refreshToken(), deviceId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(Authentication authentication) {
        authService.logoutAllDevices(authentication == null ? null : authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@RequestBody AuthChangePasswordRequest request,
                                               Authentication authentication) {
        authService.changePassword(authentication == null ? null : authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }
}
