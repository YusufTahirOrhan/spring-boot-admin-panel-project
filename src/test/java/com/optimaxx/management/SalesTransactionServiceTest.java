package com.optimaxx.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.optimaxx.management.domain.model.InventoryItem;
import com.optimaxx.management.domain.model.SaleTransaction;
import com.optimaxx.management.domain.model.TransactionType;
import com.optimaxx.management.domain.repository.SaleTransactionRepository;
import com.optimaxx.management.domain.repository.TransactionTypeRepository;
import com.optimaxx.management.interfaces.rest.dto.CreateSaleTransactionRequest;
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
        SecurityAuditService auditService = Mockito.mock(SecurityAuditService.class);
        InventoryStockCoordinator inventoryStockCoordinator = Mockito.mock(InventoryStockCoordinator.class);

        UUID typeId = UUID.randomUUID();
        TransactionType type = new TransactionType();
        type.setCode("GLASS_SALE");
        type.setName("Glass Sale");
        type.setActive(true);
        type.setCategory(com.optimaxx.management.domain.model.TransactionTypeCategory.SALE);

        when(typeRepository.findByIdAndDeletedFalse(typeId)).thenReturn(Optional.of(type));
        when(saleRepository.save(any(SaleTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SalesTransactionService service = new SalesTransactionService(saleRepository, typeRepository, auditService, inventoryStockCoordinator);
        var response = service.create(new CreateSaleTransactionRequest(typeId, "Yusuf", new BigDecimal("1500.00"), "progressive lens", null, null));

        assertThat(response.transactionTypeCode()).isEqualTo("GLASS_SALE");
        assertThat(response.customerName()).isEqualTo("Yusuf");
        verify(saleRepository).save(any(SaleTransaction.class));
    }

    @Test
    void shouldRejectInactiveTransactionType() {
        SaleTransactionRepository saleRepository = Mockito.mock(SaleTransactionRepository.class);
        TransactionTypeRepository typeRepository = Mockito.mock(TransactionTypeRepository.class);
        SecurityAuditService auditService = Mockito.mock(SecurityAuditService.class);
        InventoryStockCoordinator inventoryStockCoordinator = Mockito.mock(InventoryStockCoordinator.class);

        UUID typeId = UUID.randomUUID();
        TransactionType type = new TransactionType();
        type.setCode("GLASS_SALE");
        type.setActive(false);

        when(typeRepository.findByIdAndDeletedFalse(typeId)).thenReturn(Optional.of(type));

        SalesTransactionService service = new SalesTransactionService(saleRepository, typeRepository, auditService, inventoryStockCoordinator);

        assertThatThrownBy(() -> service.create(new CreateSaleTransactionRequest(typeId, "Yusuf", new BigDecimal("1200.00"), null, null, null)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldConsumeStockWhenInventoryPayloadPresent() {
        SaleTransactionRepository saleRepository = Mockito.mock(SaleTransactionRepository.class);
        TransactionTypeRepository typeRepository = Mockito.mock(TransactionTypeRepository.class);
        SecurityAuditService auditService = Mockito.mock(SecurityAuditService.class);
        InventoryStockCoordinator inventoryStockCoordinator = Mockito.mock(InventoryStockCoordinator.class);

        UUID typeId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        TransactionType type = new TransactionType();
        type.setCode("GLASS_SALE");
        type.setName("Glass Sale");
        type.setActive(true);
        type.setCategory(com.optimaxx.management.domain.model.TransactionTypeCategory.SALE);

        when(typeRepository.findByIdAndDeletedFalse(typeId)).thenReturn(Optional.of(type));
        when(saleRepository.save(any(SaleTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        InventoryItem item = new InventoryItem();
        item.setSku("SKU-1");
        when(inventoryStockCoordinator.consume(any(UUID.class), any(Integer.class), any(String.class), any(String.class), org.mockito.ArgumentMatchers.nullable(UUID.class), any(String.class))).thenReturn(item);

        SalesTransactionService service = new SalesTransactionService(saleRepository, typeRepository, auditService, inventoryStockCoordinator);
        service.create(new CreateSaleTransactionRequest(typeId, "Yusuf", new BigDecimal("1500.00"), "progressive lens", itemId, 2));

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
        SecurityAuditService auditService = Mockito.mock(SecurityAuditService.class);
        InventoryStockCoordinator inventoryStockCoordinator = Mockito.mock(InventoryStockCoordinator.class);

        UUID typeId = UUID.randomUUID();
        TransactionType type = new TransactionType();
        type.setCode("GLASS_SALE");
        type.setActive(true);
        type.setCategory(com.optimaxx.management.domain.model.TransactionTypeCategory.SALE);
        when(typeRepository.findByIdAndDeletedFalse(typeId)).thenReturn(Optional.of(type));

        SalesTransactionService service = new SalesTransactionService(saleRepository, typeRepository, auditService, inventoryStockCoordinator);

        assertThatThrownBy(() -> service.create(new CreateSaleTransactionRequest(typeId, "Yusuf", new BigDecimal("100.00"), null, UUID.randomUUID(), null)))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(inventoryStockCoordinator);
    }

    @Test
    void shouldListTransactions() {
        SaleTransactionRepository saleRepository = Mockito.mock(SaleTransactionRepository.class);
        TransactionTypeRepository typeRepository = Mockito.mock(TransactionTypeRepository.class);
        SecurityAuditService auditService = Mockito.mock(SecurityAuditService.class);
        InventoryStockCoordinator inventoryStockCoordinator = Mockito.mock(InventoryStockCoordinator.class);

        TransactionType type = new TransactionType();
        type.setCode("LENS_ORDER");
        type.setName("Lens Order");
        type.setActive(true);

        SaleTransaction sale = new SaleTransaction();
        sale.setTransactionType(type);
        sale.setCustomerName("Customer A");
        sale.setAmount(new BigDecimal("250.00"));
        sale.setOccurredAt(Instant.now());

        when(saleRepository.findByDeletedFalseOrderByOccurredAtDesc()).thenReturn(List.of(sale));

        SalesTransactionService service = new SalesTransactionService(saleRepository, typeRepository, auditService, inventoryStockCoordinator);

        assertThat(service.list(null)).hasSize(1);
    }
}
