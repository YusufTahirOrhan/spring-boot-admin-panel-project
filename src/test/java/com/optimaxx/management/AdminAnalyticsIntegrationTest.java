package com.optimaxx.management;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.optimaxx.management.application.AdminAnalyticsService;
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
import com.optimaxx.management.domain.repository.TransactionTypeRepository;
import com.optimaxx.management.domain.repository.UserRepository;
import com.optimaxx.management.interfaces.rest.dto.ActivityLogEventResponse;
import com.optimaxx.management.interfaces.rest.dto.AnalyticsCategoryTrendItem;
import com.optimaxx.management.interfaces.rest.dto.AnalyticsCategoryTrendResponse;
import com.optimaxx.management.interfaces.rest.dto.AnalyticsHighRiskEventResponse;
import com.optimaxx.management.interfaces.rest.dto.AnalyticsLowStockItem;
import com.optimaxx.management.interfaces.rest.dto.AnalyticsRevenueSummaryResponse;
import com.optimaxx.management.interfaces.rest.dto.AnalyticsStaffPerformanceItem;
import com.optimaxx.management.interfaces.rest.dto.AnalyticsStaffPerformanceResponse;
import com.optimaxx.management.security.jwt.JwtTokenService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
                "org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration"
})
class AdminAnalyticsIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JwtTokenService jwtTokenService;

    // ── Repository mocks (required to satisfy Spring context) ───────────────────

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
    private com.optimaxx.management.security.InventoryStockCoordinator inventoryStockCoordinator;

    // ── Service under test (real bean, NOT mocked) ───────────────────────────────
    // AdminAnalyticsService is a real Spring bean; we mock the service here
    // only to keep the test fast and isolated from repository-level SQL.

    @MockitoBean
    private AdminAnalyticsService adminAnalyticsService;

    private MockMvc mockMvc;

    private static final String BASE_URL = "/api/v1/admin/analytics";
    private static final String START = "2026-01-01T00:00:00Z";
    private static final String END   = "2026-12-31T23:59:59Z";
    private static final Instant START_INSTANT = Instant.parse(START);
    private static final Instant END_INSTANT   = Instant.parse(END);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    // ── Access control tests (STAFF must be forbidden) ────────────────────────────

    @Test
    void shouldReturnForbiddenForRevenueSummaryWhenRoleIsStaff() throws Exception {
        String staffToken = jwtTokenService.generateAccessToken("staff1", "STAFF");

        mockMvc.perform(get(BASE_URL + "/revenue")
                        .header("Authorization", "Bearer " + staffToken)
                        .param("startDate", START)
                        .param("endDate", END))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnForbiddenForCategoryTrendWhenRoleIsStaff() throws Exception {
        String staffToken = jwtTokenService.generateAccessToken("staff1", "STAFF");

        mockMvc.perform(get(BASE_URL + "/category-trend")
                        .header("Authorization", "Bearer " + staffToken)
                        .param("startDate", START)
                        .param("endDate", END))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnForbiddenForStaffPerformanceWhenRoleIsStaff() throws Exception {
        String staffToken = jwtTokenService.generateAccessToken("staff1", "STAFF");

        mockMvc.perform(get(BASE_URL + "/staff-performance")
                        .header("Authorization", "Bearer " + staffToken)
                        .param("startDate", START)
                        .param("endDate", END))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnForbiddenForLowStockWhenRoleIsStaff() throws Exception {
        String staffToken = jwtTokenService.generateAccessToken("staff1", "STAFF");

        mockMvc.perform(get(BASE_URL + "/low-stock")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnForbiddenForHighRiskEventsWhenRoleIsStaff() throws Exception {
        String staffToken = jwtTokenService.generateAccessToken("staff1", "STAFF");

        mockMvc.perform(get(BASE_URL + "/high-risk-events")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnForbiddenForAllAnalyticsEndpointsWhenTokenMissing() throws Exception {
        mockMvc.perform(get(BASE_URL + "/revenue")
                        .param("startDate", START)
                        .param("endDate", END))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/low-stock"))
                .andExpect(status().isForbidden());
    }

    // ── Happy-path tests (OWNER and ADMIN roles) ──────────────────────────────────

    @Test
    void shouldReturnRevenueSummaryForOwner() throws Exception {
        String ownerToken = jwtTokenService.generateAccessToken("owner", "OWNER");

        AnalyticsRevenueSummaryResponse response = new AnalyticsRevenueSummaryResponse(
                new BigDecimal("15000.00"), 12L, new BigDecimal("1250.00"),
                START_INSTANT, END_INSTANT);

        when(adminAnalyticsService.getRevenueSummary(any(Instant.class), any(Instant.class)))
                .thenReturn(response);

        mockMvc.perform(get(BASE_URL + "/revenue")
                        .header("Authorization", "Bearer " + ownerToken)
                        .param("startDate", START)
                        .param("endDate", END))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").value(15000.00))
                .andExpect(jsonPath("$.transactionCount").value(12))
                .andExpect(jsonPath("$.avgTransactionValue").value(1250.00));
    }

    @Test
    void shouldReturnRevenueSummaryForAdmin() throws Exception {
        String adminToken = jwtTokenService.generateAccessToken("admin1", "ADMIN");

        AnalyticsRevenueSummaryResponse response = new AnalyticsRevenueSummaryResponse(
                BigDecimal.ZERO, 0L, BigDecimal.ZERO, START_INSTANT, END_INSTANT);

        when(adminAnalyticsService.getRevenueSummary(any(Instant.class), any(Instant.class)))
                .thenReturn(response);

        mockMvc.perform(get(BASE_URL + "/revenue")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("startDate", START)
                        .param("endDate", END))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionCount").value(0));
    }

    @Test
    void shouldReturnCategoryTrendForOwner() throws Exception {
        String ownerToken = jwtTokenService.generateAccessToken("owner", "OWNER");

        AnalyticsCategoryTrendResponse response = new AnalyticsCategoryTrendResponse(
                List.of(
                        new AnalyticsCategoryTrendItem("SALE", new BigDecimal("12000.00")),
                        new AnalyticsCategoryTrendItem("REPAIR", new BigDecimal("3000.00"))
                ),
                START_INSTANT, END_INSTANT
        );

        when(adminAnalyticsService.getCategoryTrend(any(Instant.class), any(Instant.class)))
                .thenReturn(response);

        mockMvc.perform(get(BASE_URL + "/category-trend")
                        .header("Authorization", "Bearer " + ownerToken)
                        .param("startDate", START)
                        .param("endDate", END))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].category").value("SALE"))
                .andExpect(jsonPath("$.items[0].totalRevenue").value(12000.00))
                .andExpect(jsonPath("$.items[1].category").value("REPAIR"));
    }

    @Test
    void shouldReturnStaffPerformanceForOwner() throws Exception {
        String ownerToken = jwtTokenService.generateAccessToken("owner", "OWNER");

        UUID staffId = UUID.randomUUID();
        AnalyticsStaffPerformanceResponse response = new AnalyticsStaffPerformanceResponse(
                List.of(new AnalyticsStaffPerformanceItem(staffId, "john.doe", 42L)),
                START_INSTANT, END_INSTANT
        );

        when(adminAnalyticsService.getStaffPerformance(any(Instant.class), any(Instant.class)))
                .thenReturn(response);

        mockMvc.perform(get(BASE_URL + "/staff-performance")
                        .header("Authorization", "Bearer " + ownerToken)
                        .param("startDate", START)
                        .param("endDate", END))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.staff").isArray())
                .andExpect(jsonPath("$.staff[0].username").value("john.doe"))
                .andExpect(jsonPath("$.staff[0].actionCount").value(42));
    }

    @Test
    void shouldReturnLowStockAlertsForOwner() throws Exception {
        String ownerToken = jwtTokenService.generateAccessToken("owner", "OWNER");

        UUID itemId = UUID.randomUUID();
        when(adminAnalyticsService.getLowStockAlerts())
                .thenReturn(List.of(
                        new AnalyticsLowStockItem(itemId, "SKU-001", "Blue Frame", "FRAMES", 2, 5, 3)
                ));

        mockMvc.perform(get(BASE_URL + "/low-stock")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].sku").value("SKU-001"))
                .andExpect(jsonPath("$[0].quantity").value(2))
                .andExpect(jsonPath("$[0].minQuantity").value(5))
                .andExpect(jsonPath("$[0].deficit").value(3));
    }

    @Test
    void shouldReturnEmptyLowStockListWhenAllItemsAreAdequate() throws Exception {
        String ownerToken = jwtTokenService.generateAccessToken("owner", "OWNER");

        when(adminAnalyticsService.getLowStockAlerts()).thenReturn(List.of());

        mockMvc.perform(get(BASE_URL + "/low-stock")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldReturnHighRiskEventsForOwner() throws Exception {
        String ownerToken = jwtTokenService.generateAccessToken("owner", "OWNER");

        UUID eventId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        AnalyticsHighRiskEventResponse response = new AnalyticsHighRiskEventResponse(
                List.of(new ActivityLogEventResponse(
                        eventId, actorId, "ADMIN", "USER_DELETED",
                        "USER", "user-123",
                        "{}", "{\"deleted\":true}",
                        "req-1", "127.0.0.1", "MockAgent",
                        Instant.now()
                )),
                1
        );

        when(adminAnalyticsService.getHighRiskEvents(anyInt())).thenReturn(response);

        mockMvc.perform(get(BASE_URL + "/high-risk-events")
                        .header("Authorization", "Bearer " + ownerToken)
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events").isArray())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.events[0].action").value("USER_DELETED"))
                .andExpect(jsonPath("$.events[0].actorRole").value("ADMIN"));
    }

    @Test
    void shouldUseDefaultLimitForHighRiskEventsWhenParamNotProvided() throws Exception {
        String ownerToken = jwtTokenService.generateAccessToken("owner", "OWNER");

        when(adminAnalyticsService.getHighRiskEvents(anyInt()))
                .thenReturn(new AnalyticsHighRiskEventResponse(List.of(), 0));

        mockMvc.perform(get(BASE_URL + "/high-risk-events")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }
}
