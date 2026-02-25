package com.optimaxx.management;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.optimaxx.management.domain.model.RefreshToken;
import com.optimaxx.management.domain.model.User;
import com.optimaxx.management.domain.model.UserRole;
import com.optimaxx.management.domain.repository.ActivityLogRepository;
import com.optimaxx.management.domain.repository.PasswordResetTokenRepository;
import com.optimaxx.management.domain.repository.RefreshTokenRepository;
import com.optimaxx.management.domain.repository.CustomerRepository;
import com.optimaxx.management.domain.repository.InventoryItemRepository;
import com.optimaxx.management.domain.repository.InventoryMovementRepository;
import com.optimaxx.management.domain.repository.RepairOrderRepository;
import com.optimaxx.management.domain.repository.LensPrescriptionRepository;
import com.optimaxx.management.domain.repository.SaleTransactionRepository;
import com.optimaxx.management.domain.repository.TransactionTypeRepository;
import com.optimaxx.management.domain.repository.UserRepository;
import com.optimaxx.management.security.ForgotPasswordAttemptService;
import com.optimaxx.management.security.LoginAttemptService;
import com.optimaxx.management.security.jwt.JwtTokenService;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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

    @MockitoBean
    private TransactionTypeRepository transactionTypeRepository;

    @MockitoBean
    private SaleTransactionRepository saleTransactionRepository;

    @MockitoBean
    private CustomerRepository customerRepository;

    @MockitoBean
    private RepairOrderRepository repairOrderRepository;

    @MockitoBean
    private LensPrescriptionRepository lensPrescriptionRepository;

    @MockitoBean
    private InventoryItemRepository inventoryItemRepository;

    @MockitoBean
    private InventoryMovementRepository inventoryMovementRepository;

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

        when(refreshTokenRepository.findByUserAndRevokedFalseAndExpiresAtAfter(any(User.class), any(Instant.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    Instant now = invocation.getArgument(1);
                    return refreshTokenStore.values().stream()
                            .filter(token -> token.getUser() == user)
                            .filter(token -> !token.isRevoked())
                            .filter(token -> token.getExpiresAt() != null && token.getExpiresAt().isAfter(now))
                            .toList();
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
    void shouldReturnForbiddenForAuditEventsWhenRoleIsStaff() throws Exception {
        String staffToken = jwtTokenService.generateAccessToken("staff1", "STAFF");

        mockMvc.perform(get("/api/v1/admin/audit/events")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnAuditEventsForOwnerRole() throws Exception {
        String ownerToken = jwtTokenService.generateAccessToken("owner", "OWNER");

        com.optimaxx.management.domain.model.ActivityLog log = new com.optimaxx.management.domain.model.ActivityLog();
        log.setActorUserId(UUID.randomUUID());
        log.setActorRole("OWNER");
        log.setAction("LOGIN_SUCCESS");
        log.setResourceType("AUTH");
        log.setResourceId("owner");
        log.setBeforeJson("{}");
        log.setAfterJson("{}");
        log.setRequestId("req-1");
        log.setOccurredAt(Instant.now());
        log.setDeleted(false);

        when(activityLogRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(log), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/admin/audit/events")
                        .header("Authorization", "Bearer " + ownerToken)
                        .param("eventType", "LOGIN_SUCCESS")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("LOGIN_SUCCESS"));
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
    void shouldRejectExpiredPasswordResetToken() throws Exception {
        com.optimaxx.management.domain.model.PasswordResetToken expiredToken = new com.optimaxx.management.domain.model.PasswordResetToken();
        expiredToken.setUsed(false);
        expiredToken.setExpiresAt(Instant.now().minusSeconds(60));

        when(passwordResetTokenRepository.findByTokenHashAndUsedFalseAndExpiresAtAfter(anyString(), any(Instant.class)))
                .thenReturn(Optional.empty());

        String resetBody = """
                {"resetToken":"expired-token","newPassword":"newPassword123"}
                """;

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectReusedPasswordResetToken() throws Exception {
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

        when(passwordResetTokenRepository.findByTokenHashAndUsedFalseAndExpiresAtAfter(anyString(), any(Instant.class)))
                .thenReturn(Optional.of(resetToken), Optional.empty());
        when(passwordEncoder.encode("newPassword123")).thenReturn("new-hash");
        when(refreshTokenRepository.findByUserAndRevokedFalseAndExpiresAtAfter(any(User.class), any(Instant.class)))
                .thenReturn(java.util.List.of());

        String resetBody = """
                {"resetToken":"single-use-token","newPassword":"newPassword123"}
                """;

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetBody))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRevokeAllDeviceRefreshTokensAfterPasswordReset() throws Exception {
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

        String device1Login = mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Device-Id", "device-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String device2Login = mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Device-Id", "device-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        com.optimaxx.management.domain.model.PasswordResetToken resetToken = new com.optimaxx.management.domain.model.PasswordResetToken();
        resetToken.setUser(owner);
        resetToken.setUsed(false);
        resetToken.setExpiresAt(Instant.now().plusSeconds(3600));

        when(passwordResetTokenRepository.findByTokenHashAndUsedFalseAndExpiresAtAfter(anyString(), any(Instant.class)))
                .thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("newPassword123")).thenReturn("new-hash");

        String resetBody = """
                {"resetToken":"valid-token","newPassword":"newPassword123"}
                """;

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetBody))
                .andExpect(status().isNoContent());

        String refreshBodyDevice1 = """
                {"refreshToken":"%s"}
                """.formatted(extractRefreshToken(device1Login));

        String refreshBodyDevice2 = """
                {"refreshToken":"%s"}
                """.formatted(extractRefreshToken(device2Login));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-Device-Id", "device-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBodyDevice1))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-Device-Id", "device-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBodyDevice2))
                .andExpect(status().isUnauthorized());
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

    @Test
    void shouldCreateAndListSalesTransactionsForStaffRole() throws Exception {
        String staffToken = jwtTokenService.generateAccessToken("staff1", "STAFF");

        UUID transactionTypeId = UUID.randomUUID();
        com.optimaxx.management.domain.model.TransactionType transactionType = new com.optimaxx.management.domain.model.TransactionType();
        transactionType.setCode("GLASS_SALE");
        transactionType.setName("Glass Sale");
        transactionType.setActive(true);
        transactionType.setCategory(com.optimaxx.management.domain.model.TransactionTypeCategory.SALE);

        when(transactionTypeRepository.findByIdAndDeletedFalse(transactionTypeId)).thenReturn(Optional.of(transactionType));
        when(saleTransactionRepository.save(any(com.optimaxx.management.domain.model.SaleTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String createBody = """
                {"transactionTypeId":"%s","customerName":"Yusuf","amount":1200.50,"notes":"frame + lens"}
                """.formatted(transactionTypeId);

        mockMvc.perform(post("/api/v1/sales/transactions")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionTypeCode").value("GLASS_SALE"));

        com.optimaxx.management.domain.model.SaleTransaction listed = new com.optimaxx.management.domain.model.SaleTransaction();
        listed.setTransactionType(transactionType);
        listed.setCustomerName("Yusuf");
        listed.setAmount(new java.math.BigDecimal("1200.50"));
        listed.setOccurredAt(Instant.now());
        when(saleTransactionRepository.findByDeletedFalseOrderByOccurredAtDesc()).thenReturn(java.util.List.of(listed));

        mockMvc.perform(get("/api/v1/sales/transactions")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerName").value("Yusuf"));
    }

    @Test
    void shouldRejectSalesTransactionForInactiveType() throws Exception {
        String staffToken = jwtTokenService.generateAccessToken("staff1", "STAFF");

        UUID transactionTypeId = UUID.randomUUID();
        com.optimaxx.management.domain.model.TransactionType transactionType = new com.optimaxx.management.domain.model.TransactionType();
        transactionType.setCode("REPAIR");
        transactionType.setName("Repair");
        transactionType.setActive(false);
        transactionType.setCategory(com.optimaxx.management.domain.model.TransactionTypeCategory.REPAIR);

        when(transactionTypeRepository.findByIdAndDeletedFalse(transactionTypeId)).thenReturn(Optional.of(transactionType));

        String createBody = """
                {"transactionTypeId":"%s","customerName":"Yusuf","amount":1200.50}
                """.formatted(transactionTypeId);

        mockMvc.perform(post("/api/v1/sales/transactions")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldDeleteSalesCustomerForStaffRole() throws Exception {
        String staffToken = jwtTokenService.generateAccessToken("staff1", "STAFF");
        UUID customerId = UUID.randomUUID();

        com.optimaxx.management.domain.model.Customer customer = new com.optimaxx.management.domain.model.Customer();
        customer.setDeleted(false);

        when(customerRepository.findByIdAndDeletedFalse(customerId)).thenReturn(Optional.of(customer));
        when(saleTransactionRepository.existsByCustomerAndDeletedFalse(customer)).thenReturn(false);
        when(repairOrderRepository.existsByCustomerAndDeletedFalse(customer)).thenReturn(false);
        when(lensPrescriptionRepository.existsByCustomerAndDeletedFalse(customer)).thenReturn(false);

        mockMvc.perform(delete("/api/v1/sales/customers/{id}", customerId)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldRejectDeleteSalesCustomerWhenLinkedRecordsExist() throws Exception {
        String staffToken = jwtTokenService.generateAccessToken("staff1", "STAFF");
        UUID customerId = UUID.randomUUID();

        com.optimaxx.management.domain.model.Customer customer = new com.optimaxx.management.domain.model.Customer();
        customer.setDeleted(false);

        when(customerRepository.findByIdAndDeletedFalse(customerId)).thenReturn(Optional.of(customer));
        when(saleTransactionRepository.existsByCustomerAndDeletedFalse(customer)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/sales/customers/{id}", customerId)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldCreateAndListSalesCustomersForStaffRole() throws Exception {
        String staffToken = jwtTokenService.generateAccessToken("staff1", "STAFF");

        com.optimaxx.management.domain.model.Customer saved = new com.optimaxx.management.domain.model.Customer();
        saved.setFirstName("Yusuf");
        saved.setLastName("Orhan");
        saved.setPhone("555");
        saved.setEmail("yusuf@example.com");
        saved.setNotes("vip");

        when(customerRepository.save(any(com.optimaxx.management.domain.model.Customer.class))).thenReturn(saved);

        String createBody = """
                {"firstName":"Yusuf","lastName":"Orhan","phone":"555","email":"yusuf@example.com","notes":"vip"}
                """;

        mockMvc.perform(post("/api/v1/sales/customers")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Yusuf"));

        when(customerRepository.findByDeletedFalseOrderByCreatedAtDesc()).thenReturn(java.util.List.of(saved));

        mockMvc.perform(get("/api/v1/sales/customers")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lastName").value("Orhan"));
    }

    @Test
    void shouldRejectSalesCustomerCreateForMissingNames() throws Exception {
        String staffToken = jwtTokenService.generateAccessToken("staff1", "STAFF");

        String createBody = """
                {"firstName":"","lastName":""}
                """;

        mockMvc.perform(post("/api/v1/sales/customers")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldCreateListAndUpdateRepairOrdersForStaffRole() throws Exception {
        String staffToken = jwtTokenService.generateAccessToken("staff1", "STAFF");

        UUID customerId = UUID.randomUUID();
        UUID transactionTypeId = UUID.randomUUID();
        UUID repairOrderId = UUID.randomUUID();

        com.optimaxx.management.domain.model.Customer customer = new com.optimaxx.management.domain.model.Customer();
        customer.setFirstName("Yusuf");
        customer.setLastName("Orhan");

        com.optimaxx.management.domain.model.TransactionType type = new com.optimaxx.management.domain.model.TransactionType();
        type.setCode("FRAME_REPAIR");
        type.setActive(true);
        type.setCategory(com.optimaxx.management.domain.model.TransactionTypeCategory.REPAIR);

        when(customerRepository.findByIdAndDeletedFalse(customerId)).thenReturn(Optional.of(customer));
        when(transactionTypeRepository.findByIdAndDeletedFalse(transactionTypeId)).thenReturn(Optional.of(type));

        com.optimaxx.management.domain.model.RepairOrder saved = new com.optimaxx.management.domain.model.RepairOrder();
        saved.setCustomer(customer);
        saved.setTransactionType(type);
        saved.setTitle("Temple Fix");
        saved.setStatus(com.optimaxx.management.domain.model.RepairStatus.RECEIVED);
        saved.setReceivedAt(Instant.now());

        when(repairOrderRepository.save(any(com.optimaxx.management.domain.model.RepairOrder.class))).thenReturn(saved);

        String createBody = """
                {"customerId":"%s","transactionTypeId":"%s","title":"Temple Fix","description":"left side loose"}
                """.formatted(customerId, transactionTypeId);

        mockMvc.perform(post("/api/v1/sales/repairs")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Temple Fix"));

        when(repairOrderRepository.findByDeletedFalseOrderByReceivedAtDesc()).thenReturn(java.util.List.of(saved));

        mockMvc.perform(get("/api/v1/sales/repairs")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("RECEIVED"));

        when(repairOrderRepository.findByIdAndDeletedFalse(any(java.util.UUID.class))).thenReturn(Optional.of(saved));

        mockMvc.perform(patch("/api/v1/sales/repairs/{id}/status", repairOrderId)
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void shouldCreateRepairWithReservationAndReleaseOnCancel() throws Exception {
        String staffToken = jwtTokenService.generateAccessToken("staff1", "STAFF");

        UUID customerId = UUID.randomUUID();
        UUID transactionTypeId = UUID.randomUUID();
        UUID repairOrderId = UUID.randomUUID();
        UUID inventoryItemId = UUID.randomUUID();

        com.optimaxx.management.domain.model.Customer customer = new com.optimaxx.management.domain.model.Customer();
        customer.setFirstName("Yusuf");
        customer.setLastName("Orhan");

        com.optimaxx.management.domain.model.TransactionType type = new com.optimaxx.management.domain.model.TransactionType();
        type.setCode("FRAME_REPAIR");
        type.setActive(true);
        type.setCategory(com.optimaxx.management.domain.model.TransactionTypeCategory.REPAIR);

        com.optimaxx.management.domain.model.InventoryItem item = new com.optimaxx.management.domain.model.InventoryItem();
        item.setSku("SKU-1");
        item.setQuantity(10);

        com.optimaxx.management.domain.model.RepairOrder saved = new com.optimaxx.management.domain.model.RepairOrder();
        saved.setCustomer(customer);
        saved.setTransactionType(type);
        saved.setTitle("Temple Fix");
        saved.setStatus(com.optimaxx.management.domain.model.RepairStatus.RECEIVED);
        saved.setReceivedAt(Instant.now());
        saved.setReservedInventoryItemId(inventoryItemId);
        saved.setReservedInventoryQuantity(2);
        saved.setInventoryReleased(false);

        when(customerRepository.findByIdAndDeletedFalse(customerId)).thenReturn(Optional.of(customer));
        when(transactionTypeRepository.findByIdAndDeletedFalse(transactionTypeId)).thenReturn(Optional.of(type));
        when(inventoryItemRepository.findByIdAndDeletedFalse(inventoryItemId)).thenReturn(Optional.of(item));
        when(inventoryMovementRepository.findByIdempotencyKeyAndDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(repairOrderRepository.save(any(com.optimaxx.management.domain.model.RepairOrder.class))).thenReturn(saved);

        String createBody = """
                {"customerId":"%s","transactionTypeId":"%s","title":"Temple Fix","description":"left side loose","inventoryItemId":"%s","inventoryQuantity":2}
                """.formatted(customerId, transactionTypeId, inventoryItemId);

        mockMvc.perform(post("/api/v1/sales/repairs")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservedInventoryItemId").value(inventoryItemId.toString()))
                .andExpect(jsonPath("$.reservedInventoryQuantity").value(2))
                .andExpect(jsonPath("$.inventoryReleased").value(false));

        when(repairOrderRepository.findByIdAndDeletedFalse(repairOrderId)).thenReturn(Optional.of(saved));

        mockMvc.perform(patch("/api/v1/sales/repairs/{id}/status", repairOrderId)
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.inventoryReleased").value(true));
    }

    @Test
    void shouldRejectInvalidRepairStatusTransition() throws Exception {
        String staffToken = jwtTokenService.generateAccessToken("staff1", "STAFF");

        UUID repairOrderId = UUID.randomUUID();
        com.optimaxx.management.domain.model.RepairOrder order = new com.optimaxx.management.domain.model.RepairOrder();
        com.optimaxx.management.domain.model.Customer customer = new com.optimaxx.management.domain.model.Customer();
        com.optimaxx.management.domain.model.TransactionType type = new com.optimaxx.management.domain.model.TransactionType();
        order.setCustomer(customer);
        order.setTransactionType(type);
        order.setTitle("Temple Fix");
        order.setStatus(com.optimaxx.management.domain.model.RepairStatus.RECEIVED);
        order.setReceivedAt(Instant.now());

        when(repairOrderRepository.findByIdAndDeletedFalse(repairOrderId)).thenReturn(Optional.of(order));

        mockMvc.perform(patch("/api/v1/sales/repairs/{id}/status", repairOrderId)
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DELIVERED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldCreateAndChangeStockForAdminRole() throws Exception {
        String adminToken = jwtTokenService.generateAccessToken("admin1", "ADMIN");

        com.optimaxx.management.domain.model.InventoryItem item = new com.optimaxx.management.domain.model.InventoryItem();
        item.setSku("SKU-1");
        item.setName("Lens");
        item.setQuantity(10);
        item.setMinQuantity(2);

        when(inventoryItemRepository.existsBySkuAndDeletedFalse("SKU-1")).thenReturn(false);
        when(inventoryItemRepository.save(any(com.optimaxx.management.domain.model.InventoryItem.class))).thenReturn(item);

        mockMvc.perform(post("/api/v1/admin/inventory/items")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"sku-1\",\"name\":\"Lens\",\"quantity\":10,\"minQuantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-1"));

        when(inventoryItemRepository.findByDeletedFalseOrderByNameAsc()).thenReturn(java.util.List.of(item));

        mockMvc.perform(get("/api/v1/admin/inventory/items")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Lens"));

        when(inventoryItemRepository.findByIdAndDeletedFalse(any(java.util.UUID.class))).thenReturn(Optional.of(item));
        when(inventoryMovementRepository.save(any(com.optimaxx.management.domain.model.InventoryMovement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/v1/admin/inventory/items/{id}/stock", java.util.UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movementType\":\"OUT\",\"quantity\":3,\"reason\":\"sale\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(7));
    }

    @Test
    void shouldCreateAndListPrescriptionsForStaffRole() throws Exception {
        String staffToken = jwtTokenService.generateAccessToken("staff1", "STAFF");

        UUID customerId = UUID.randomUUID();
        UUID transactionTypeId = UUID.randomUUID();

        com.optimaxx.management.domain.model.Customer customer = new com.optimaxx.management.domain.model.Customer();
        customer.setStoreId(UUID.randomUUID());

        com.optimaxx.management.domain.model.TransactionType type = new com.optimaxx.management.domain.model.TransactionType();
        type.setStoreId(UUID.randomUUID());
        type.setActive(true);
        type.setCategory(com.optimaxx.management.domain.model.TransactionTypeCategory.PRESCRIPTION);

        com.optimaxx.management.domain.model.LensPrescription saved = new com.optimaxx.management.domain.model.LensPrescription();
        saved.setStoreId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        saved.setCustomer(customer);
        saved.setTransactionType(type);
        saved.setRightSphere("-1.25");
        saved.setRecordedAt(Instant.now());

        when(customerRepository.findByIdAndDeletedFalse(customerId)).thenReturn(Optional.of(customer));
        when(transactionTypeRepository.findByIdAndDeletedFalse(transactionTypeId)).thenReturn(Optional.of(type));
        when(lensPrescriptionRepository.save(any(com.optimaxx.management.domain.model.LensPrescription.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/sales/prescriptions")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":"%s","transactionTypeId":"%s","rightSphere":"-1.25"}
                                """.formatted(customerId, transactionTypeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rightSphere").value("-1.25"));

        when(lensPrescriptionRepository.findByDeletedFalseOrderByRecordedAtDesc()).thenReturn(java.util.List.of(saved));

        mockMvc.perform(get("/api/v1/sales/prescriptions")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rightSphere").value("-1.25"));
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
