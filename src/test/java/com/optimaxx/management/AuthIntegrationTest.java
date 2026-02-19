package com.optimaxx.management;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.optimaxx.management.domain.model.RefreshToken;
import com.optimaxx.management.domain.model.User;
import com.optimaxx.management.domain.model.UserRole;
import com.optimaxx.management.domain.repository.ActivityLogRepository;
import com.optimaxx.management.domain.repository.PasswordResetTokenRepository;
import com.optimaxx.management.domain.repository.RefreshTokenRepository;
import com.optimaxx.management.domain.repository.UserRepository;
import com.optimaxx.management.security.ForgotPasswordAttemptService;
import com.optimaxx.management.security.LoginAttemptService;
import com.optimaxx.management.security.jwt.JwtTokenService;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
                "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration," +
                "org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration",
        "security.forgot-password-protection.max-requests=2",
        "security.forgot-password-protection.window-minutes=5"
})
class AuthIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private ForgotPasswordAttemptService forgotPasswordAttemptService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private ActivityLogRepository activityLogRepository;

    @MockitoBean
    private PasswordResetTokenRepository passwordResetTokenRepository;

    private MockMvc mockMvc;
    private final Map<String, RefreshToken> refreshTokenStore = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        refreshTokenStore.clear();
        loginAttemptService.clearAll();
        forgotPasswordAttemptService.clearAll();

        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            refreshTokenStore.put(token.getTokenHash(), token);
            return token;
        });

        when(refreshTokenRepository.findByTokenHashAndRevokedFalseAndExpiresAtAfter(anyString(), any(Instant.class)))
                .thenAnswer(invocation -> {
                    String tokenHash = invocation.getArgument(0);
                    Instant now = invocation.getArgument(1);
                    RefreshToken token = refreshTokenStore.get(tokenHash);
                    if (token == null || token.isRevoked() || token.getExpiresAt().isBefore(now)) {
                        return Optional.empty();
                    }
                    return Optional.of(token);
                });
    }

    @Test
    void shouldReturnForbiddenForAdminEndpointWhenTokenMissing() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/" + UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnForbiddenForAdminEndpointWhenRoleIsStaff() throws Exception {
        String staffToken = jwtTokenService.generateAccessToken("staff1", "STAFF");

        mockMvc.perform(get("/api/v1/admin/users/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldLoginRefreshAndInvalidateOldRefreshToken() throws Exception {
        User owner = new User();
        owner.setUsername("owner");
        owner.setEmail("owner@optimaxx.local");
        owner.setPasswordHash("hashed-pass");
        owner.setRole(UserRole.OWNER);
        owner.setActive(true);
        owner.setDeleted(false);
        owner.setStoreId(UUID.randomUUID());

        when(userRepository.findByUsernameAndDeletedFalse("owner")).thenReturn(Optional.of(owner));
        when(passwordEncoder.matches("owner12345", "hashed-pass")).thenReturn(true);

        String loginBody = """
                {"username":"owner","password":"owner12345"}
                """;

        String loginResponseBody = mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Device-Id", "device-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String refreshToken = extractRefreshToken(loginResponseBody);

        String refreshBody = """
                {"refreshToken":"%s"}
                """.formatted(refreshToken);

        String rotatedResponseBody = mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-Device-Id", "device-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-Device-Id", "device-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isUnauthorized());

        String rotatedRefreshToken = extractRefreshToken(rotatedResponseBody);
        String logoutBody = """
                {"refreshToken":"%s"}
                """.formatted(rotatedRefreshToken);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("X-Device-Id", "device-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logoutBody))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-Device-Id", "device-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logoutBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnTooManyRequestsAfterRepeatedInvalidLogins() throws Exception {
        User owner = new User();
        owner.setUsername("owner");
        owner.setEmail("owner@optimaxx.local");
        owner.setPasswordHash("hashed-pass");
        owner.setRole(UserRole.OWNER);
        owner.setActive(true);
        owner.setDeleted(false);
        owner.setStoreId(UUID.randomUUID());

        when(userRepository.findByUsernameAndDeletedFalse("owner")).thenReturn(Optional.of(owner));
        when(passwordEncoder.matches("wrong", "hashed-pass")).thenReturn(false);

        String loginBody = """
                {"username":"owner","password":"wrong"}
                """;

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .header("X-Device-Id", "device-1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Device-Id", "device-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void shouldReturnAuditRetryStatusForOwnerRole() throws Exception {
        String ownerToken = jwtTokenService.generateAccessToken("owner", "OWNER");

        mockMvc.perform(get("/api/v1/admin/audit/retry-status")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingQueueSize").exists())
                .andExpect(jsonPath("$.droppedCount").exists())
                .andExpect(jsonPath("$.publishFailureCount").exists())
                .andExpect(jsonPath("$.retryAttemptCount").exists())
                .andExpect(jsonPath("$.publishedSuccessCount").exists());
    }


    @Test
    void shouldLogoutAllDevicesForAuthenticatedUser() throws Exception {
        String ownerToken = jwtTokenService.generateAccessToken("owner", "OWNER");

        User owner = new User();
        owner.setUsername("owner");
        owner.setRole(UserRole.OWNER);
        owner.setStoreId(UUID.randomUUID());
        owner.setDeleted(false);

        when(userRepository.findByUsernameAndDeletedFalse("owner")).thenReturn(Optional.of(owner));
        when(refreshTokenRepository.findByUserAndRevokedFalseAndExpiresAtAfter(any(User.class), any(Instant.class)))
                .thenReturn(java.util.List.of());

        mockMvc.perform(post("/api/v1/auth/logout-all")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldListDeviceSessionsForAuthenticatedUser() throws Exception {
        String ownerToken = jwtTokenService.generateAccessToken("owner", "OWNER");

        User owner = new User();
        owner.setUsername("owner");
        owner.setRole(UserRole.OWNER);
        owner.setStoreId(UUID.randomUUID());
        owner.setDeleted(false);

        RefreshToken token = new RefreshToken();
        token.setUser(owner);
        token.setDeviceId("device-1");
        token.setExpiresAt(Instant.now().plusSeconds(3600));

        when(userRepository.findByUsernameAndDeletedFalse("owner")).thenReturn(Optional.of(owner));
        when(refreshTokenRepository.findByUserAndRevokedFalseAndExpiresAtAfter(any(User.class), any(Instant.class)))
                .thenReturn(java.util.List.of(token));

        mockMvc.perform(get("/api/v1/auth/devices")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Device-Id", "device-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deviceId").value("device-1"))
                .andExpect(jsonPath("$[0].current").value(true));
    }

    @Test
    void shouldLogoutSpecificDeviceForAuthenticatedUser() throws Exception {
        String ownerToken = jwtTokenService.generateAccessToken("owner", "OWNER");

        User owner = new User();
        owner.setUsername("owner");
        owner.setRole(UserRole.OWNER);
        owner.setStoreId(UUID.randomUUID());
        owner.setDeleted(false);

        RefreshToken token = new RefreshToken();
        token.setUser(owner);
        token.setDeviceId("device-1");
        token.setRevoked(false);
        token.setExpiresAt(Instant.now().plusSeconds(3600));

        when(userRepository.findByUsernameAndDeletedFalse("owner")).thenReturn(Optional.of(owner));
        when(refreshTokenRepository.findByUserAndDeviceIdAndRevokedFalseAndExpiresAtAfter(any(User.class), anyString(), any(Instant.class)))
                .thenReturn(java.util.List.of(token));

        mockMvc.perform(post("/api/v1/auth/logout-device")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\":\"device-1\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldRequestAndCompletePasswordResetFlow() throws Exception {
        User owner = new User();
        owner.setUsername("owner");
        owner.setEmail("owner@optimaxx.local");
        owner.setRole(UserRole.OWNER);
        owner.setActive(true);
        owner.setDeleted(false);
        owner.setStoreId(UUID.randomUUID());

        com.optimaxx.management.domain.model.PasswordResetToken resetToken = new com.optimaxx.management.domain.model.PasswordResetToken();
        resetToken.setUser(owner);
        resetToken.setUsed(false);
        resetToken.setExpiresAt(Instant.now().plusSeconds(3600));

        when(userRepository.findByEmailAndDeletedFalse("owner@optimaxx.local")).thenReturn(Optional.of(owner));
        when(passwordResetTokenRepository.findByUserAndUsedFalseAndExpiresAtAfter(any(User.class), any(Instant.class)))
                .thenReturn(java.util.List.of());
        when(passwordResetTokenRepository.save(any(com.optimaxx.management.domain.model.PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordResetTokenRepository.findByTokenHashAndUsedFalseAndExpiresAtAfter(anyString(), any(Instant.class)))
                .thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("newPassword123")).thenReturn("new-hash");
        when(refreshTokenRepository.findByUserAndRevokedFalseAndExpiresAtAfter(any(User.class), any(Instant.class)))
                .thenReturn(java.util.List.of());

        String forgotBody = """
                {"email":"owner@optimaxx.local"}
                """;

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(forgotBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());

        String resetBody = """
                {"resetToken":"any-token","newPassword":"newPassword123"}
                """;

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetBody))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldRateLimitForgotPasswordRequests() throws Exception {
        String forgotBody = """
                {"email":"owner@optimaxx.local"}
                """;

        when(userRepository.findByEmailAndDeletedFalse("owner@optimaxx.local")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(forgotBody))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(forgotBody))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(forgotBody))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void shouldRejectSelfDeleteForOwner() throws Exception {
        User owner = new User();
        UUID ownerId = UUID.randomUUID();
        owner.setUsername("owner");
        owner.setEmail("owner@optimaxx.local");
        owner.setPasswordHash("hashed-pass");
        owner.setRole(UserRole.OWNER);
        owner.setActive(true);
        owner.setDeleted(false);
        owner.setStoreId(UUID.randomUUID());

        User sameOwner = new User();
        sameOwner.setUsername("owner");
        sameOwner.setEmail("owner@optimaxx.local");
        sameOwner.setPasswordHash("hashed-pass");
        sameOwner.setRole(UserRole.OWNER);
        sameOwner.setActive(true);
        sameOwner.setDeleted(false);
        sameOwner.setStoreId(UUID.randomUUID());

        when(userRepository.findByUsernameAndDeletedFalse("owner")).thenReturn(Optional.of(owner));
        when(userRepository.findByIdAndDeletedFalse(ownerId)).thenReturn(Optional.of(sameOwner));

        String ownerToken = jwtTokenService.generateAccessToken("owner", "OWNER");

        mockMvc.perform(delete("/api/v1/admin/users/" + ownerId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isForbidden());
    }

    private String extractRefreshToken(String json) {
        int keyIndex = json.indexOf("\"refreshToken\":\"");
        if (keyIndex < 0) {
            throw new IllegalStateException("refreshToken not found in response");
        }
        int start = keyIndex + "\"refreshToken\":\"".length();
        int end = json.indexOf('"', start);
        return json.substring(start, end);
    }
}
