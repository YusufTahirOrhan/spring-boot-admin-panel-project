package com.optimaxx.management;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.optimaxx.management.application.AdminAnalyticsService;
import com.optimaxx.management.domain.model.ActivityLog;
import com.optimaxx.management.domain.repository.ActivityLogRepository;
import com.optimaxx.management.domain.repository.CustomerRepository;
import com.optimaxx.management.domain.repository.InventoryItemRepository;
import com.optimaxx.management.domain.repository.InventoryMovementRepository;
import com.optimaxx.management.domain.repository.LeadRepository;
import com.optimaxx.management.domain.repository.LensPrescriptionRepository;
import com.optimaxx.management.domain.repository.PasswordResetTokenRepository;
import com.optimaxx.management.domain.repository.RefreshTokenRepository;
import com.optimaxx.management.domain.repository.RepairOrderRepository;
import com.optimaxx.management.domain.repository.SaleTransactionRepository;
import com.optimaxx.management.domain.repository.SitePageBlockRepository;
import com.optimaxx.management.domain.repository.TransactionTypeRepository;
import com.optimaxx.management.domain.repository.UserRepository;
import com.optimaxx.management.security.InventoryStockCoordinator;
import com.optimaxx.management.security.jwt.JwtTokenService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(properties = {
        "clickhouse.url=",
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
                "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration," +
                "org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration"
})
class AdminAuditIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;

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

    @MockitoBean
    private LeadRepository leadRepository;

    @MockitoBean
    private SitePageBlockRepository sitePageBlockRepository;

    @MockitoBean
    private AdminAnalyticsService adminAnalyticsService;

    @MockitoBean
    private InventoryStockCoordinator inventoryStockCoordinator;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void shouldServeAdminAuditEventsFromPostgresActivityLogs() throws Exception {
        ActivityLog log = new ActivityLog();
        log.setActorUserId(UUID.randomUUID());
        log.setActorRole("OWNER");
        log.setAction("LOGIN_SUCCESS");
        log.setResourceType("AUTH");
        log.setResourceId("owner");
        log.setBeforeJson("{}");
        log.setAfterJson("{}");
        log.setRequestId("req-1");
        log.setIpAddress("127.0.0.1");
        log.setUserAgent("JUnit");
        log.setOccurredAt(Instant.parse("2026-06-30T09:00:00Z"));
        log.setStoreId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        log.setDeleted(false);

        when(activityLogRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log), PageRequest.of(0, 20), 1));

        String token = jwtTokenService.generateAccessToken("owner", "OWNER");

        mockMvc.perform(get("/api/v1/admin/audit/events")
                        .header("Authorization", "Bearer " + token)
                        .param("eventType", "LOGIN_SUCCESS")
                        .param("resourceType", "AUTH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("LOGIN_SUCCESS"))
                .andExpect(jsonPath("$.content[0].resourceType").value("AUTH"));

        verify(activityLogRepository).findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class));
    }
}
