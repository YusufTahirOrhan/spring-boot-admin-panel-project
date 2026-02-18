package com.optimaxx.management.interfaces.rest;

import com.optimaxx.management.interfaces.rest.dto.AdminCreateUserRequest;
import com.optimaxx.management.interfaces.rest.dto.AdminUpdateUserRoleRequest;
import com.optimaxx.management.interfaces.rest.dto.AdminUpdateUserStatusRequest;
import com.optimaxx.management.interfaces.rest.dto.UserResponse;
import com.optimaxx.management.security.UserManagementService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final UserManagementService userManagementService;

    public AdminUserController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@RequestBody AdminCreateUserRequest request) {
        return userManagementService.createUser(request);
    }

    @GetMapping("/{userId}")
    public UserResponse getUser(@PathVariable UUID userId) {
        return userManagementService.getUser(userId);
    }

    @PatchMapping("/{userId}/role")
    public UserResponse updateRole(@PathVariable UUID userId, @RequestBody AdminUpdateUserRoleRequest request) {
        return userManagementService.updateRole(userId, request == null ? null : request.role());
    }

    @PatchMapping("/{userId}/status")
    public UserResponse updateStatus(@PathVariable UUID userId, @RequestBody AdminUpdateUserStatusRequest request) {
        return userManagementService.updateStatus(userId, request != null && request.active());
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId, Authentication authentication) {
        userManagementService.softDeleteUser(userId, authentication == null ? null : authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
