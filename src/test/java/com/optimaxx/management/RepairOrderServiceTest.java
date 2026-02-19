package com.optimaxx.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.optimaxx.management.domain.model.Customer;
import com.optimaxx.management.domain.model.InventoryItem;
import com.optimaxx.management.domain.model.RepairOrder;
import com.optimaxx.management.domain.model.RepairStatus;
import com.optimaxx.management.domain.model.TransactionType;
import com.optimaxx.management.domain.repository.CustomerRepository;
import com.optimaxx.management.domain.repository.RepairOrderRepository;
import com.optimaxx.management.domain.repository.TransactionTypeRepository;
import com.optimaxx.management.interfaces.rest.dto.CreateRepairOrderRequest;
import com.optimaxx.management.interfaces.rest.dto.UpdateRepairStatusRequest;
import com.optimaxx.management.security.InventoryStockCoordinator;
import com.optimaxx.management.security.RepairOrderService;
import com.optimaxx.management.security.audit.SecurityAuditService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

class RepairOrderServiceTest {

    @Test
    void shouldCreateRepairOrder() {
        RepairOrderRepository repairOrderRepository = Mockito.mock(RepairOrderRepository.class);
        CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
        TransactionTypeRepository transactionTypeRepository = Mockito.mock(TransactionTypeRepository.class);
        SecurityAuditService securityAuditService = Mockito.mock(SecurityAuditService.class);
        InventoryStockCoordinator inventoryStockCoordinator = Mockito.mock(InventoryStockCoordinator.class);

        UUID customerId = UUID.randomUUID();
        UUID transactionTypeId = UUID.randomUUID();

        Customer customer = new Customer();
        customer.setFirstName("Yusuf");
        customer.setLastName("Orhan");

        TransactionType type = new TransactionType();
        type.setCode("FRAME_REPAIR");
        type.setActive(true);
        type.setCategory(com.optimaxx.management.domain.model.TransactionTypeCategory.REPAIR);

        when(customerRepository.findByIdAndDeletedFalse(customerId)).thenReturn(Optional.of(customer));
        when(transactionTypeRepository.findByIdAndDeletedFalse(transactionTypeId)).thenReturn(Optional.of(type));
        when(repairOrderRepository.save(any(RepairOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RepairOrderService service = new RepairOrderService(repairOrderRepository, customerRepository, transactionTypeRepository, securityAuditService, inventoryStockCoordinator);

        var response = service.create(new CreateRepairOrderRequest(customerId, transactionTypeId, "Temple Fix", "left side loose", null, null));

        assertThat(response.title()).isEqualTo("Temple Fix");
        assertThat(response.status()).isEqualTo(RepairStatus.RECEIVED);
        assertThat(response.reservedInventoryItemId()).isNull();
        assertThat(response.reservedInventoryQuantity()).isNull();
        assertThat(response.inventoryReleased()).isFalse();
        verify(repairOrderRepository).save(any(RepairOrder.class));
    }

    @Test
    void shouldRejectInactiveTransactionType() {
        RepairOrderRepository repairOrderRepository = Mockito.mock(RepairOrderRepository.class);
        CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
        TransactionTypeRepository transactionTypeRepository = Mockito.mock(TransactionTypeRepository.class);
        SecurityAuditService securityAuditService = Mockito.mock(SecurityAuditService.class);
        InventoryStockCoordinator inventoryStockCoordinator = Mockito.mock(InventoryStockCoordinator.class);

        UUID customerId = UUID.randomUUID();
        UUID transactionTypeId = UUID.randomUUID();

        when(customerRepository.findByIdAndDeletedFalse(customerId)).thenReturn(Optional.of(new Customer()));

        TransactionType type = new TransactionType();
        type.setActive(false);
        type.setCategory(com.optimaxx.management.domain.model.TransactionTypeCategory.REPAIR);
        when(transactionTypeRepository.findByIdAndDeletedFalse(transactionTypeId)).thenReturn(Optional.of(type));

        RepairOrderService service = new RepairOrderService(repairOrderRepository, customerRepository, transactionTypeRepository, securityAuditService, inventoryStockCoordinator);

        assertThatThrownBy(() -> service.create(new CreateRepairOrderRequest(customerId, transactionTypeId, "Fix", null, null, null)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldConsumeStockWhenInventoryPayloadPresent() {
        RepairOrderRepository repairOrderRepository = Mockito.mock(RepairOrderRepository.class);
        CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
        TransactionTypeRepository transactionTypeRepository = Mockito.mock(TransactionTypeRepository.class);
        SecurityAuditService securityAuditService = Mockito.mock(SecurityAuditService.class);
        InventoryStockCoordinator inventoryStockCoordinator = Mockito.mock(InventoryStockCoordinator.class);

        UUID customerId = UUID.randomUUID();
        UUID transactionTypeId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        Customer customer = new Customer();
        when(customerRepository.findByIdAndDeletedFalse(customerId)).thenReturn(Optional.of(customer));

        TransactionType type = new TransactionType();
        type.setCode("FRAME_REPAIR");
        type.setActive(true);
        type.setCategory(com.optimaxx.management.domain.model.TransactionTypeCategory.REPAIR);
        when(transactionTypeRepository.findByIdAndDeletedFalse(transactionTypeId)).thenReturn(Optional.of(type));
        when(repairOrderRepository.save(any(RepairOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        InventoryItem item = new InventoryItem();
        item.setSku("SKU-1");
        when(inventoryStockCoordinator.consume(any(UUID.class), any(Integer.class), any(String.class), any(String.class), org.mockito.ArgumentMatchers.nullable(UUID.class), any(String.class))).thenReturn(item);

        RepairOrderService service = new RepairOrderService(repairOrderRepository, customerRepository, transactionTypeRepository, securityAuditService, inventoryStockCoordinator);
        service.create(new CreateRepairOrderRequest(customerId, transactionTypeId, "Temple Fix", "left side loose", itemId, 1));

        verify(inventoryStockCoordinator).consume(
                org.mockito.ArgumentMatchers.eq(itemId),
                org.mockito.ArgumentMatchers.eq(1),
                org.mockito.ArgumentMatchers.contains("REPAIR reservation"),
                org.mockito.ArgumentMatchers.eq("REPAIR_ORDER_RESERVATION"),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.contains("null:reserve")
        );
    }

    @Test
    void shouldRejectPartialInventoryPayload() {
        RepairOrderRepository repairOrderRepository = Mockito.mock(RepairOrderRepository.class);
        CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
        TransactionTypeRepository transactionTypeRepository = Mockito.mock(TransactionTypeRepository.class);
        SecurityAuditService securityAuditService = Mockito.mock(SecurityAuditService.class);
        InventoryStockCoordinator inventoryStockCoordinator = Mockito.mock(InventoryStockCoordinator.class);

        UUID customerId = UUID.randomUUID();
        UUID transactionTypeId = UUID.randomUUID();

        when(customerRepository.findByIdAndDeletedFalse(customerId)).thenReturn(Optional.of(new Customer()));

        TransactionType type = new TransactionType();
        type.setActive(true);
        type.setCategory(com.optimaxx.management.domain.model.TransactionTypeCategory.REPAIR);
        when(transactionTypeRepository.findByIdAndDeletedFalse(transactionTypeId)).thenReturn(Optional.of(type));

        RepairOrderService service = new RepairOrderService(repairOrderRepository, customerRepository, transactionTypeRepository, securityAuditService, inventoryStockCoordinator);

        assertThatThrownBy(() -> service.create(new CreateRepairOrderRequest(customerId, transactionTypeId, "Fix", null, UUID.randomUUID(), null)))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(inventoryStockCoordinator);
    }

    @Test
    void shouldReleaseReservedStockWhenCanceled() {
        RepairOrderRepository repairOrderRepository = Mockito.mock(RepairOrderRepository.class);
        CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
        TransactionTypeRepository transactionTypeRepository = Mockito.mock(TransactionTypeRepository.class);
        SecurityAuditService securityAuditService = Mockito.mock(SecurityAuditService.class);
        InventoryStockCoordinator inventoryStockCoordinator = Mockito.mock(InventoryStockCoordinator.class);

        UUID orderId = UUID.randomUUID();
        UUID inventoryItemId = UUID.randomUUID();
        RepairOrder order = new RepairOrder();
        Customer customer = new Customer();
        TransactionType type = new TransactionType();
        order.setCustomer(customer);
        order.setTransactionType(type);
        order.setStatus(RepairStatus.IN_PROGRESS);
        order.setTitle("Repair");
        order.setReservedInventoryItemId(inventoryItemId);
        order.setReservedInventoryQuantity(2);
        order.setInventoryReleased(false);

        InventoryItem item = new InventoryItem();
        item.setSku("SKU-1");

        when(repairOrderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.of(order));
        when(inventoryStockCoordinator.release(any(UUID.class), any(Integer.class), any(String.class), any(String.class), org.mockito.ArgumentMatchers.nullable(UUID.class), any(String.class)))
                .thenReturn(item);

        RepairOrderService service = new RepairOrderService(repairOrderRepository, customerRepository, transactionTypeRepository, securityAuditService, inventoryStockCoordinator);

        var response = service.updateStatus(orderId, new UpdateRepairStatusRequest(RepairStatus.CANCELED));

        assertThat(response.status()).isEqualTo(RepairStatus.CANCELED);
        assertThat(order.isInventoryReleased()).isTrue();
        verify(inventoryStockCoordinator).release(
                org.mockito.ArgumentMatchers.eq(inventoryItemId),
                org.mockito.ArgumentMatchers.eq(2),
                org.mockito.ArgumentMatchers.contains("release"),
                org.mockito.ArgumentMatchers.eq("REPAIR_ORDER_RELEASE"),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.contains(":release")
        );
    }

    @Test
    void shouldRejectInvalidStatusTransition() {
        RepairOrderRepository repairOrderRepository = Mockito.mock(RepairOrderRepository.class);
        CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
        TransactionTypeRepository transactionTypeRepository = Mockito.mock(TransactionTypeRepository.class);
        SecurityAuditService securityAuditService = Mockito.mock(SecurityAuditService.class);
        InventoryStockCoordinator inventoryStockCoordinator = Mockito.mock(InventoryStockCoordinator.class);

        UUID orderId = UUID.randomUUID();
        RepairOrder order = new RepairOrder();
        order.setStatus(RepairStatus.RECEIVED);
        order.setCustomer(new Customer());
        order.setTransactionType(new TransactionType());
        order.setTitle("Repair");

        when(repairOrderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.of(order));

        RepairOrderService service = new RepairOrderService(repairOrderRepository, customerRepository, transactionTypeRepository, securityAuditService, inventoryStockCoordinator);

        assertThatThrownBy(() -> service.updateStatus(orderId, new UpdateRepairStatusRequest(RepairStatus.DELIVERED)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldNotReleaseStockWhenDelivered() {
        RepairOrderRepository repairOrderRepository = Mockito.mock(RepairOrderRepository.class);
        CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
        TransactionTypeRepository transactionTypeRepository = Mockito.mock(TransactionTypeRepository.class);
        SecurityAuditService securityAuditService = Mockito.mock(SecurityAuditService.class);
        InventoryStockCoordinator inventoryStockCoordinator = Mockito.mock(InventoryStockCoordinator.class);

        UUID orderId = UUID.randomUUID();
        RepairOrder order = new RepairOrder();
        order.setCustomer(new Customer());
        order.setTransactionType(new TransactionType());
        order.setTitle("Repair");
        order.setStatus(RepairStatus.READY_FOR_PICKUP);
        order.setReservedInventoryItemId(UUID.randomUUID());
        order.setReservedInventoryQuantity(1);
        order.setInventoryReleased(false);

        when(repairOrderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.of(order));

        RepairOrderService service = new RepairOrderService(repairOrderRepository, customerRepository, transactionTypeRepository, securityAuditService, inventoryStockCoordinator);
        var response = service.updateStatus(orderId, new UpdateRepairStatusRequest(RepairStatus.DELIVERED));

        assertThat(response.status()).isEqualTo(RepairStatus.DELIVERED);
        verifyNoInteractions(inventoryStockCoordinator);
    }

    @Test
    void shouldUpdateRepairStatus() {
        RepairOrderRepository repairOrderRepository = Mockito.mock(RepairOrderRepository.class);
        CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
        TransactionTypeRepository transactionTypeRepository = Mockito.mock(TransactionTypeRepository.class);
        SecurityAuditService securityAuditService = Mockito.mock(SecurityAuditService.class);
        InventoryStockCoordinator inventoryStockCoordinator = Mockito.mock(InventoryStockCoordinator.class);

        UUID orderId = UUID.randomUUID();
        RepairOrder order = new RepairOrder();
        Customer customer = new Customer();
        TransactionType type = new TransactionType();
        order.setCustomer(customer);
        order.setTransactionType(type);
        order.setStatus(RepairStatus.RECEIVED);
        order.setTitle("Repair");

        when(repairOrderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.of(order));
        when(repairOrderRepository.findByDeletedFalseOrderByReceivedAtDesc()).thenReturn(List.of(order));

        RepairOrderService service = new RepairOrderService(repairOrderRepository, customerRepository, transactionTypeRepository, securityAuditService, inventoryStockCoordinator);

        var response = service.updateStatus(orderId, new UpdateRepairStatusRequest(RepairStatus.IN_PROGRESS));

        assertThat(response.status()).isEqualTo(RepairStatus.IN_PROGRESS);
    }
}
