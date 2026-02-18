package com.optimaxx.management.interfaces.rest;

import com.optimaxx.management.interfaces.rest.dto.AuthChangePasswordRequest;
import com.optimaxx.management.interfaces.rest.dto.AuthLoginRequest;
import com.optimaxx.management.interfaces.rest.dto.AuthLoginResponse;
import com.optimaxx.management.interfaces.rest.dto.AuthLogoutRequest;
import com.optimaxx.management.interfaces.rest.dto.AuthRefreshRequest;
import com.optimaxx.management.interfaces.rest.dto.DeviceSessionResponse;
import com.optimaxx.management.interfaces.rest.dto.ForgotPasswordRequest;
import com.optimaxx.management.interfaces.rest.dto.ForgotPasswordResponse;
import com.optimaxx.management.interfaces.rest.dto.LogoutDeviceRequest;
import com.optimaxx.management.interfaces.rest.dto.ResetPasswordRequest;
import com.optimaxx.management.security.AuthService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/devices")
    public List<DeviceSessionResponse> listDevices(Authentication authentication,
                                                   @RequestHeader(value = "X-Device-Id", required = false) String currentDeviceId) {
        return authService.listDeviceSessions(authentication == null ? null : authentication.getName(), currentDeviceId);
    }

    @PostMapping("/logout-device")
    public ResponseEntity<Void> logoutDevice(@RequestBody LogoutDeviceRequest request,
                                             Authentication authentication) {
        authService.logoutDevice(authentication == null ? null : authentication.getName(), request == null ? null : request.deviceId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ForgotPasswordResponse forgotPassword(@RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request == null ? null : request.email());
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@RequestBody AuthChangePasswordRequest request,
                                               Authentication authentication) {
        authService.changePassword(authentication == null ? null : authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }
}
