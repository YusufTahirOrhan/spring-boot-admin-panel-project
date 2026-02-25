package com.optimaxx.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.optimaxx.management.domain.model.Customer;
import com.optimaxx.management.domain.model.InventoryItem;
import com.optimaxx.management.domain.model.SalePaymentMethod;
import com.optimaxx.management.domain.model.SaleTransaction;
import com.optimaxx.management.domain.model.TransactionType;
import com.optimaxx.management.domain.model.TransactionTypeCategory;
import com.optimaxx.management.domain.repository.ActivityLogRepository;
import com.optimaxx.management.domain.repository.CustomerRepository;
import com.optimaxx.management.domain.repository.SaleTransactionRepository;
import com.optimaxx.management.domain.repository.TransactionTypeRepository;
import com.optimaxx.management.domain.model.SaleTransactionStatus;
import com.optimaxx.management.interfaces.rest.dto.CreateSaleTransactionRequest;
import com.optimaxx.management.interfaces.rest.dto.RefundSaleTransactionRequest;
import com.optimaxx.management.interfaces.rest.dto.UpdateSaleTransactionStatusRequest;
import com.optimaxx.management.security.InventoryStockCoordinator;
import com.optimaxx.management.security.SalesTransactionService;
import com.optimaxx.management.security.audit.SecurityAuditService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

class SalesTransactionServiceTest {

    @Test
    void shouldCreateSaleTransaction() {
        SaleTransactionRepository saleRepository = Mockito.mock(SaleTransactionRepository.class);
        TransactionTypeRepository typeRepository = Mockito.mock(TransactionTypeRepository.class);
        CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
        SecurityAuditService auditService = Mockito.mock(SecurityAuditService.class);
        ActivityLogRepository activityLogRepository = Mockito.mock(ActivityLogRepository.class);
        InventoryStockCoordinator inventoryStockCoordinator = Mockito.mock(InventoryStockCoordinator.class);

        UUID typeId = UUID.randomUUID();
        TransactionType type = new TransactionType();
        type.setCode("GLASS_SALE");
        type.setName("Glass Sale");
        type.setActive(true);
        type.setCategory(TransactionTypeCategory.SALE);

        when(typeRepository.findByIdAndDeletedFalse(typeId)).thenReturn(Optional.of(type));
        when(saleRepository.findTopByStoreIdAndReceiptNumberStartingWithOrderByReceiptNumberDesc(any(UUID.class), any(String.class))).thenReturn(Optional.empty());
        when(saleRepository.existsByStoreIdAndReceiptNumberAndDeletedFalse(any(UUID.class), any(String.class))).thenReturn(false);
        when(saleRepository.save(any(SaleTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SalesTransactionService service = new SalesTransactionService(
                saleRepository,
                typeRepository,
                customerRepository,
                activityLogRepository,
                auditService,
                inventoryStockCoordinator
        );
        var response = service.create(new CreateSaleTransactionRequest(typeId, null, "Yusuf", new BigDecimal("1500.00"), "progressive lens", null, null, null, null));

        assertThat(response.transactionTypeCode()).isEqualTo("GLASS_SALE");
        assertThat(response.customerName()).isEqualTo("Yusuf");
        assertThat(response.receiptNumber()).startsWith("RCP-");
        assertThat(response.status()).isEqualTo("COMPLETED");
        verify(saleRepository).save(any(SaleTransaction.class));
    }

    @Test
    void shouldLinkCustomerWhenCustomerIdProvided() {
        SaleTransactionRepository saleRepository = Mockito.mock(SaleTransactionRepository.class);
        TransactionTypeRepository typeRepository = Mockito.mock(TransactionTypeRepository.class);
        CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
        SecurityAuditService auditService = Mockito.mock(SecurityAuditService.class);
        ActivityLogRepository activityLogRepository = Mockito.mock(ActivityLogRepository.class);
        InventoryStockCoordinator inventoryStockCoordinator = Mockito.mock(InventoryStockCoordinator.class);

        UUID typeId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        TransactionType type = new TransactionType();
        type.setCode("GLASS_SALE");
        type.setName("Glass Sale");
        type.setActive(true);
        type.setCategory(TransactionTypeCategory.SALE);

        Customer customer = new Customer();
        customer.setStoreId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        customer.setFirstName("Yusuf");
        customer.setLastName("Orhan");

        when(typeRepository.findByIdAndDeletedFalse(typeId)).thenReturn(Optional.of(type));
        when(customerRepository.findByIdAndDeletedFalse(customerId)).thenReturn(Optional.of(customer));
        when(saleRepository.findTopByStoreIdAndReceiptNumberStartingWithOrderByReceiptNumberDesc(any(UUID.class), any(String.class))).thenReturn(Optional.empty());
        when(saleRepository.existsByStoreIdAndReceiptNumberAndDeletedFalse(any(UUID.class), any(String.class))).thenReturn(false);
        when(saleRepository.save(any(SaleTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SalesTransactionService service = new SalesTransactionService(
                saleRepository,
                typeRepository,
                customerRepository,
                activityLogRepository,
                auditService,
                inventoryStockCoordinator
        );

        var response = service.create(new CreateSaleTransactionRequest(typeId, customerId, null, new BigDecimal("1500.00"), null, null, null, null, null));

        assertThat(response.customerName()).isEqualTo("Yusuf Orhan");
    }

    @Test
    void shouldRejectInactiveTransactionType() {
        SaleTransactionRepository saleRepository = Mockito.mock(SaleTransactionRepository.class);
        TransactionTypeRepository typeRepository = Mockito.mock(TransactionTypeRepository.class);
        CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
        SecurityAuditService auditService = Mockito.mock(SecurityAuditService.class);
        ActivityLogRepository activityLogRepository = Mockito.mock(ActivityLogRepository.class);
        InventoryStockCoordinator inventoryStockCoordinator = Mockito.mock(InventoryStockCoordinator.class);

        UUID typeId = UUID.randomUUID();
        TransactionType type = new TransactionType();
        type.setCode("GLASS_SALE");
        type.setActive(false);

        when(typeRepository.findByIdAndDeletedFalse(typeId)).thenReturn(Optional.of(type));

        SalesTransactionService service = new SalesTransactionService(saleRepository, typeRepository, customerRepository, activityLogRepository, auditService, inventoryStockCoordinator);

        assertThatThrownBy(() -> service.create(new CreateSaleTransactionRequest(typeId, null, "Yusuf", new BigDecimal("1200.00"), null, null, null, null, null)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldConsumeStockWhenInventoryPayloadPresent() {
        SaleTransactionRepository saleRepository = Mockito.mock(SaleTransactionRepository.class);
        TransactionTypeRepository typeRepository = Mockito.mock(TransactionTypeRepository.class);
        CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
        SecurityAuditService auditService = Mockito.mock(SecurityAuditService.class);
        ActivityLogRepository activityLogRepository = Mockito.mock(ActivityLogRepository.class);
        InventoryStockCoordinator inventoryStockCoordinator = Mockito.mock(InventoryStockCoordinator.class);

        UUID typeId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        TransactionType type = new TransactionType();
        type.setCode("GLASS_SALE");
        type.setName("Glass Sale");
        type.setActive(true);
        type.setCategory(TransactionTypeCategory.SALE);

        when(typeRepository.findByIdAndDeletedFalse(typeId)).thenReturn(Optional.of(type));
        when(saleRepository.findTopByStoreIdAndReceiptNumberStartingWithOrderByReceiptNumberDesc(any(UUID.class), any(String.class))).thenReturn(Optional.empty());
        when(saleRepository.existsByStoreIdAndReceiptNumberAndDeletedFalse(any(UUID.class), any(String.class))).thenReturn(false);
        when(saleRepository.save(any(SaleTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        InventoryItem item = new InventoryItem();        item.setSku("SKU-1");
        when(inventoryStockCoordinator.consume(any(UUID.class), any(Integer.class), any(String.class), any(String.class), org.mockito.ArgumentMatchers.nullable(UUID.class), any(String.class))).thenReturn(item);

        SalesTransactionService service = new SalesTransactionService(saleRepository, typeRepository, customerRepository, activityLogRepository, auditService, inventoryStockCoordinator);
        service.create(new CreateSaleTransactionRequest(typeId, null, "Yusuf", new BigDecimal("1500.00"), "progressive lens", "CARD", "POS-REF-1", itemId, 2));

        verify(inventoryStockCoordinator).consume(
                org.mockito.ArgumentMatchers.eq(itemId),
                org.mockito.ArgumentMatchers.eq(2),
                org.mockito.ArgumentMatchers.contains("SALE transaction"),
                org.mockito.ArgumentMatchers.eq("SALE_TRANSACTION"),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.contains("null:consume")
        );
    }

    @Test
    void shouldRejectPartialInventoryPayload() {
        SaleTransactionRepository saleRepository = Mockito.mock(SaleTransactionRepository.class);
        TransactionTypeRepository typeRepository = Mockito.mock(TransactionTypeRepository.class);
        CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
        SecurityAuditService auditService = Mockito.mock(SecurityAuditService.class);
        ActivityLogRepository activityLogRepository = Mockito.mock(ActivityLogRepository.class);
        InventoryStockCoordinator inventoryStockCoordinator = Mockito.mock(InventoryStockCoordinator.class);

        UUID typeId = UUID.randomUUID();
        TransactionType type = new TransactionType();
        type.setCode("GLASS_SALE");
        type.setActive(true);
        type.setCategory(TransactionTypeCategory.SALE);
        when(typeRepository.findByIdAndDeletedFalse(typeId)).thenReturn(Optional.of(type));

        SalesTransactionService service = new SalesTransactionService(saleRepository, typeRepository, customerRepository, activityLogRepository, auditService, inventoryStockCoordinator);

        assertThatThrownBy(() -> service.create(new CreateSaleTransactionRequest(typeId, null, "Yusuf", new BigDecimal("100.00"), null, null, null, UUID.randomUUID(), null)))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(inventoryStockCoordinator);
    }

    @Test
    void shouldCancelSaleTransactionAndRevertStock() {
        SaleTransactionRepository saleRepository = Mockito.mock(SaleTransactionRepository.class);
        TransactionTypeRepository typeRepository = Mockito.mock(TransactionTypeRepository.class);
        CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
        SecurityAuditService auditService = Mockito.mock(SecurityAuditService.class);
        ActivityLogRepository activityLogRepository = Mockito.mock(ActivityLogRepository.class);
        InventoryStockCoordinator inventoryStockCoordinator = Mockito.mock(InventoryStockCoordinator.class);

        UUID txId = UUID.randomUUID();
        UUID inventoryItemId = UUID.randomUUID();

        TransactionType type = new TransactionType();
        type.setCode("GLASS_SALE");

        SaleTransaction transaction = new SaleTransaction();
        transaction.setTransactionType(type);
        transaction.setStatus(SaleTransactionStatus.COMPLETED);
        transaction.setInventoryItemId(inventoryItemId);
        transaction.setInventoryQuantity(2);
        transaction.setStockReverted(false);

        when(saleRepository.findByIdAndDeletedFalse(txId)).thenReturn(Optional.of(transaction));
        when(inventoryStockCoordinator.release(any(UUID.class), any(Integer.class), any(String.class), any(String.class), any(UUID.class), any(String.class)))
                .thenReturn(new InventoryItem());

        SalesTransactionService service = new SalesTransactionService(saleRepository, typeRepository, customerRepository, activityLogRepository, auditService, inventoryStockCoordinator);
        var response = service.updateStatus(txId, new UpdateSaleTransactionStatusRequest(SaleTransactionStatus.CANCELED));

        assertThat(response.status()).isEqualTo("CANCELED");
        assertThat(transaction.isStockReverted()).isTrue();
    }

    @Test
    void shouldRefundSaleTransaction() {
        SaleTransactionRepository saleRepository = Mockito.mock(SaleTransactionRepository.class);
        TransactionTypeRepository typeRepository = Mockito.mock(TransactionTypeRepository.class);
        CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
        SecurityAuditService auditService = Mockito.mock(SecurityAuditService.class);
        ActivityLogRepository activityLogRepository = Mockito.mock(ActivityLogRepository.class);
        InventoryStockCoordinator inventoryStockCoordinator = Mockito.mock(InventoryStockCoordinator.class);

        UUID txId = UUID.randomUUID();
        UUID inventoryItemId = UUID.randomUUID();

        TransactionType type = new TransactionType();
        type.setCode("GLASS_SALE");

        SaleTransaction transaction = new SaleTransaction();
        transaction.setTransactionType(type);
        transaction.setStatus(SaleTransactionStatus.COMPLETED);
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setInventoryItemId(inventoryItemId);
        transaction.setInventoryQuantity(1);

        when(saleRepository.findByIdAndDeletedFalse(txId)).thenReturn(Optional.of(transaction));
        when(inventoryStockCoordinator.release(any(UUID.class), any(Integer.class), any(String.class), any(String.class), any(UUID.class), any(String.class)))
                .thenReturn(new InventoryItem());

        SalesTransactionService service = new SalesTransactionService(saleRepository, typeRepository, customerRepository, activityLogRepository, auditService, inventoryStockCoordinator);
        var response = service.refund(txId, new RefundSaleTransactionRequest(new BigDecimal("100.00"), "return"));

        assertThat(response.status()).isEqualTo("REFUNDED");
        assertThat(response.refundedAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void shouldReturnSummaryWithPaymentBreakdown() {
        SaleTransactionRepository saleRepository = Mockito.mock(SaleTransactionRepository.class);
        TransactionTypeRepository typeRepository = Mockito.mock(TransactionTypeRepository.class);
        CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
        SecurityAuditService auditService = Mockito.mock(SecurityAuditService.class);
        ActivityLogRepository activityLogRepository = Mockito.mock(ActivityLogRepository.class);
        InventoryStockCoordinator inventoryStockCoordinator = Mockito.mock(InventoryStockCoordinator.class);

        TransactionType type = new TransactionType();
        type.setCode("GLASS_SALE");

        SaleTransaction card = new SaleTransaction();
        card.setStoreId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        card.setTransactionType(type);
        card.setAmount(new BigDecimal("200.00"));
        card.setRefundedAmount(new BigDecimal("50.00"));
        card.setPaymentMethod(SalePaymentMethod.CARD);
        card.setOccurredAt(Instant.now());

        SaleTransaction cash = new SaleTransaction();
        cash.setStoreId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        cash.setTransactionType(type);
        cash.setAmount(new BigDecimal("100.00"));
        cash.setPaymentMethod(SalePaymentMethod.CASH);
        cash.setOccurredAt(Instant.now());

        when(saleRepository.findByDeletedFalseOrderByOccurredAtDesc()).thenReturn(List.of(card, cash));

        SalesTransactionService service = new SalesTransactionService(saleRepository, typeRepository, customerRepository, activityLogRepository, auditService, inventoryStockCoordinator);
        var summary = service.summary(null, null, null);

        assertThat(summary.transactionCount()).isEqualTo(2);
        assertThat(summary.grossAmount()).isEqualByComparingTo("300.00");
        assertThat(summary.refundedAmount()).isEqualByComparingTo("50.00");
        assertThat(summary.netAmount()).isEqualByComparingTo("250.00");
        assertThat(summary.paymentMethodBreakdown()).hasSize(2);
    }

    @Test
    void shouldReturnTransactionDetailWithTimeline() {
        SaleTransactionRepository saleRepository = Mockito.mock(SaleTransactionRepository.class);
        TransactionTypeRepository typeRepository = Mockito.mock(TransactionTypeRepository.class);
        CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
        SecurityAuditService auditService = Mockito.mock(SecurityAuditService.class);
        ActivityLogRepository activityLogRepository = Mockito.mock(ActivityLogRepository.class);
        InventoryStockCoordinator inventoryStockCoordinator = Mockito.mock(InventoryStockCoordinator.class);

        UUID txId = UUID.randomUUID();
        TransactionType type = new TransactionType();
        type.setCode("GLASS_SALE");

        SaleTransaction tx = new SaleTransaction();
        tx.setStoreId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        tx.setTransactionType(type);
        tx.setAmount(new BigDecimal("100.00"));
        tx.setOccurredAt(Instant.now());
        tx.setStatus(SaleTransactionStatus.COMPLETED);

        com.optimaxx.management.domain.model.ActivityLog log = new com.optimaxx.management.domain.model.ActivityLog();
        log.setAction("SALE_TRANSACTION_CREATED");
        log.setResourceType("SALE_TRANSACTION");
        log.setResourceId(txId.toString());
        log.setOccurredAt(Instant.now());

        when(saleRepository.findByIdAndDeletedFalse(txId)).thenReturn(Optional.of(tx));
        when(activityLogRepository.findByStoreIdAndResourceTypeAndResourceIdAndDeletedFalseOrderByOccurredAtDesc(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "SALE_TRANSACTION",
                txId.toString())).thenReturn(List.of(log));

        SalesTransactionService service = new SalesTransactionService(saleRepository, typeRepository, customerRepository, activityLogRepository, auditService, inventoryStockCoordinator);
        var detail = service.detail(txId);

        assertThat(detail.id()).isEqualTo(txId);
        assertThat(detail.timeline()).hasSize(1);
        assertThat(detail.timeline().get(0).eventType()).isEqualTo("SALE_TRANSACTION_CREATED");
    }

    @Test
    void shouldListTransactions() {
        SaleTransactionRepository saleRepository = Mockito.mock(SaleTransactionRepository.class);
        TransactionTypeRepository typeRepository = Mockito.mock(TransactionTypeRepository.class);
        CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
        SecurityAuditService auditService = Mockito.mock(SecurityAuditService.class);
        ActivityLogRepository activityLogRepository = Mockito.mock(ActivityLogRepository.class);
        InventoryStockCoordinator inventoryStockCoordinator = Mockito.mock(InventoryStockCoordinator.class);

        TransactionType type = new TransactionType();
        type.setCode("LENS_ORDER");
        type.setName("Lens Order");
        type.setActive(true);

        SaleTransaction sale = new SaleTransaction();
        sale.setStoreId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        sale.setTransactionType(type);
        sale.setCustomerName("Customer A");
        sale.setReceiptNumber("RCP-20260225-0001");
        sale.setAmount(new BigDecimal("250.00"));
        sale.setPaymentMethod(com.optimaxx.management.domain.model.SalePaymentMethod.CARD);
        sale.setOccurredAt(Instant.now());

        when(saleRepository.findByDeletedFalseOrderByOccurredAtDesc()).thenReturn(List.of(sale));

        SalesTransactionService service = new SalesTransactionService(saleRepository, typeRepository, customerRepository, activityLogRepository, auditService, inventoryStockCoordinator);

        assertThat(service.list(null, null, null, null, 0, 20, "occurredAt,desc").getContent()).hasSize(1);
        assertThat(service.list(null, null, "20260225", null, 0, 20, "occurredAt,desc").getContent()).hasSize(1);
        assertThat(service.list(null, null, "missing", null, 0, 20, "occurredAt,desc").getContent()).isEmpty();
        assertThat(service.list(null, null, null, "CARD", 0, 20, "occurredAt,desc").getContent()).hasSize(1);
        assertThat(service.list(null, null, null, "CASH", 0, 20, "occurredAt,desc").getContent()).isEmpty();
    }
}
