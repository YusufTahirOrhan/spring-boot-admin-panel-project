package com.optimaxx.management;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
                "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration," +
                "org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration"
})
@ActiveProfiles("test")
public class PublicEndpointsIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.optimaxx.management.domain.repository.InventoryItemRepository inventoryItemRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.optimaxx.management.domain.repository.LeadRepository leadRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.optimaxx.management.domain.repository.ActivityLogRepository activityLogRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.optimaxx.management.domain.repository.UserRepository userRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.optimaxx.management.domain.repository.TransactionTypeRepository transactionTypeRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.optimaxx.management.domain.repository.SaleTransactionRepository saleTransactionRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.optimaxx.management.domain.repository.RepairOrderRepository repairOrderRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.optimaxx.management.domain.repository.LensPrescriptionRepository lensPrescriptionRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.optimaxx.management.domain.repository.CustomerRepository customerRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.optimaxx.management.domain.repository.InventoryMovementRepository inventoryMovementRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.optimaxx.management.domain.repository.RefreshTokenRepository refreshTokenRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.optimaxx.management.domain.repository.PasswordResetTokenRepository passwordResetTokenRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.optimaxx.management.security.InventoryStockCoordinator inventoryStockCoordinator;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void shouldReturnStoreInfoWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/public/store-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("OptiMaxx Central"));
    }

    @Test
    void shouldReturnServicesWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/public/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("SRV-01"));
    }

    @Test
    void shouldAcceptContactForm() throws Exception {
        String contactJson = """
                {
                    "name": "John Doe",
                    "email": "john@example.com",
                    "message": "I need a glasses repair.",
                    "serviceInterest": "Frame Repair"
                }
                """;

        mockMvc.perform(post("/api/v1/public/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contactJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void shouldReturnBadRequestForInvalidContactForm() throws Exception {
        String invalidJson = """
                {
                    "name": "",
                    "email": "not-an-email"
                }
                """;

        mockMvc.perform(post("/api/v1/public/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}
