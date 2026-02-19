package com.optimaxx.management;

import com.optimaxx.management.domain.repository.ActivityLogRepository;
import com.optimaxx.management.domain.repository.PasswordResetTokenRepository;
import com.optimaxx.management.domain.repository.RefreshTokenRepository;
import com.optimaxx.management.domain.repository.CustomerRepository;
import com.optimaxx.management.domain.repository.InventoryItemRepository;
import com.optimaxx.management.domain.repository.InventoryMovementRepository;
import com.optimaxx.management.domain.repository.RepairOrderRepository;
import com.optimaxx.management.domain.repository.SaleTransactionRepository;
import com.optimaxx.management.domain.repository.TransactionTypeRepository;
import com.optimaxx.management.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
                "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration," +
                "org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration"
})
class OptiMaxxManagementApplicationTests {

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
    private InventoryItemRepository inventoryItemRepository;

    @MockitoBean
    private InventoryMovementRepository inventoryMovementRepository;

    @Test
    void contextLoads() {
    }
}
