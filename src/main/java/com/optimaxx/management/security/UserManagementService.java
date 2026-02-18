package com.optimaxx.management.security;

import com.optimaxx.management.domain.model.User;
import com.optimaxx.management.domain.model.UserRole;
import com.optimaxx.management.domain.repository.UserRepository;
import com.optimaxx.management.interfaces.rest.dto.AdminCreateUserRequest;
import com.optimaxx.management.interfaces.rest.dto.UserResponse;
import jakarta.annotation.PostConstruct;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class UserManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapOwnerProperties bootstrapOwnerProperties;

    public UserManagementService(UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 BootstrapOwnerProperties bootstrapOwnerProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapOwnerProperties = bootstrapOwnerProperties;
    }

    @PostConstruct
    @Transactional
    public void bootstrapOwnerIfMissing() {
        if (!bootstrapOwnerProperties.enabled() || isBlank(bootstrapOwnerProperties.username())
                || isBlank(bootstrapOwnerProperties.email()) || isBlank(bootstrapOwnerProperties.password())) {
            return;
        }

        var existingOwner = userRepository.findByUsernameAndDeletedFalse(bootstrapOwnerProperties.username().trim());
        if (existingOwner != null && existingOwner.isPresent()) {
            return;
        }

        User owner = new User();
        owner.setUsername(bootstrapOwnerProperties.username().trim());
        owner.setEmail(bootstrapOwnerProperties.email().trim().toLowerCase());
        owner.setPasswordHash(passwordEncoder.encode(bootstrapOwnerProperties.password()));
        owner.setRole(UserRole.OWNER);
        owner.setActive(true);
        owner.setDeleted(false);
        owner.setStoreId(UUID.randomUUID());

        userRepository.save(owner);
    }

    @Transactional
    public UserResponse createUser(AdminCreateUserRequest request) {
        if (request == null || isBlank(request.username()) || isBlank(request.email()) || isBlank(request.password())) {
            throw new ResponseStatusException(BAD_REQUEST, "Username, email and password are required");
        }

        String username = request.username().trim();
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByUsernameAndDeletedFalse(username)) {
            throw new ResponseStatusException(BAD_REQUEST, "Username already exists");
        }

        if (userRepository.existsByEmailAndDeletedFalse(email)) {
            throw new ResponseStatusException(BAD_REQUEST, "Email already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role() == null ? UserRole.STAFF : request.role());
        user.setActive(request.active() == null || request.active());
        user.setDeleted(false);
        user.setStoreId(UUID.randomUUID());

        return toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(UUID userId) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));
        return toResponse(user);
    }

    @Transactional
    public UserResponse updateRole(UUID userId, UserRole role) {
        if (role == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Role is required");
        }

        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));
        user.setRole(role);
        return toResponse(user);
    }

    @Transactional
    public UserResponse updateStatus(UUID userId, boolean active) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));
        user.setActive(active);
        return toResponse(user);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole(), user.isActive());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
