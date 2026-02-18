package com.optimaxx.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.optimaxx.management.domain.model.User;
import com.optimaxx.management.domain.model.UserRole;
import com.optimaxx.management.domain.repository.UserRepository;
import com.optimaxx.management.interfaces.rest.dto.AdminCreateUserRequest;
import com.optimaxx.management.interfaces.rest.dto.UserResponse;
import com.optimaxx.management.security.BootstrapOwnerProperties;
import com.optimaxx.management.security.UserManagementService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class UserManagementServiceTest {

    @Test
    void shouldCreateUserWhenRequestIsValid() {
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
        BootstrapOwnerProperties bootstrapOwnerProperties = new BootstrapOwnerProperties(false, null, null, null);

        when(userRepository.existsByUsernameAndDeletedFalse("staff")).thenReturn(false);
        when(userRepository.existsByEmailAndDeletedFalse("staff@optimaxx.local")).thenReturn(false);
        when(passwordEncoder.encode("strong123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setCreatedAt(java.time.Instant.now());
            return user;
        });

        UserManagementService service = new UserManagementService(userRepository, passwordEncoder, bootstrapOwnerProperties);

        UserResponse response = service.createUser(
                new AdminCreateUserRequest("staff", "staff@optimaxx.local", "strong123", UserRole.STAFF, true)
        );

        assertThat(response.username()).isEqualTo("staff");
        assertThat(response.role()).isEqualTo(UserRole.STAFF);
        assertThat(response.active()).isTrue();
    }

    @Test
    void shouldRejectCreateWhenUsernameAlreadyExists() {
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
        BootstrapOwnerProperties bootstrapOwnerProperties = new BootstrapOwnerProperties(false, null, null, null);

        when(userRepository.existsByUsernameAndDeletedFalse("staff")).thenReturn(true);

        UserManagementService service = new UserManagementService(userRepository, passwordEncoder, bootstrapOwnerProperties);

        assertThatThrownBy(() -> service.createUser(
                new AdminCreateUserRequest("staff", "staff@optimaxx.local", "strong123", UserRole.STAFF, true)
        )).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldUpdateRoleForExistingUser() {
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
        BootstrapOwnerProperties bootstrapOwnerProperties = new BootstrapOwnerProperties(false, null, null, null);

        User user = new User();
        user.setUsername("staff");
        user.setEmail("staff@optimaxx.local");
        user.setRole(UserRole.STAFF);
        user.setStoreId(UUID.randomUUID());
        user.setDeleted(false);

        UUID userId = UUID.randomUUID();
        when(userRepository.findByIdAndDeletedFalse(userId)).thenReturn(Optional.of(user));

        UserManagementService service = new UserManagementService(userRepository, passwordEncoder, bootstrapOwnerProperties);

        UserResponse response = service.updateRole(userId, UserRole.ADMIN);

        assertThat(response.role()).isEqualTo(UserRole.ADMIN);
    }
}
