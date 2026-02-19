package com.optimaxx.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.optimaxx.management.domain.model.Customer;
import com.optimaxx.management.domain.model.RepairOrder;
import com.optimaxx.management.domain.model.RepairStatus;
import com.optimaxx.management.domain.model.TransactionType;
import com.optimaxx.management.domain.repository.CustomerRepository;
import com.optimaxx.management.domain.repository.RepairOrderRepository;
import com.optimaxx.management.domain.repository.TransactionTypeRepository;
import com.optimaxx.management.interfaces.rest.dto.CreateRepairOrderRequest;
import com.optimaxx.management.interfaces.rest.dto.UpdateRepairStatusRequest;
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

        RepairOrderService service = new RepairOrderService(repairOrderRepository, customerRepository, transactionTypeRepository, securityAuditService);

        var response = service.create(new CreateRepairOrderRequest(customerId, transactionTypeId, "Temple Fix", "left side loose"));

        assertThat(response.title()).isEqualTo("Temple Fix");
        assertThat(response.status()).isEqualTo(RepairStatus.RECEIVED);
        verify(repairOrderRepository).save(any(RepairOrder.class));
    }

    @Test
    void shouldRejectInactiveTransactionType() {
        RepairOrderRepository repairOrderRepository = Mockito.mock(RepairOrderRepository.class);
        CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
        TransactionTypeRepository transactionTypeRepository = Mockito.mock(TransactionTypeRepository.class);
        SecurityAuditService securityAuditService = Mockito.mock(SecurityAuditService.class);

        UUID customerId = UUID.randomUUID();
        UUID transactionTypeId = UUID.randomUUID();

        when(customerRepository.findByIdAndDeletedFalse(customerId)).thenReturn(Optional.of(new Customer()));

        TransactionType type = new TransactionType();
        type.setActive(false);
        type.setCategory(com.optimaxx.management.domain.model.TransactionTypeCategory.REPAIR);
        when(transactionTypeRepository.findByIdAndDeletedFalse(transactionTypeId)).thenReturn(Optional.of(type));

        RepairOrderService service = new RepairOrderService(repairOrderRepository, customerRepository, transactionTypeRepository, securityAuditService);

        assertThatThrownBy(() -> service.create(new CreateRepairOrderRequest(customerId, transactionTypeId, "Fix", null)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldUpdateRepairStatus() {
        RepairOrderRepository repairOrderRepository = Mockito.mock(RepairOrderRepository.class);
        CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
        TransactionTypeRepository transactionTypeRepository = Mockito.mock(TransactionTypeRepository.class);
        SecurityAuditService securityAuditService = Mockito.mock(SecurityAuditService.class);

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

        RepairOrderService service = new RepairOrderService(repairOrderRepository, customerRepository, transactionTypeRepository, securityAuditService);

        var response = service.updateStatus(orderId, new UpdateRepairStatusRequest(RepairStatus.IN_PROGRESS));

        assertThat(response.status()).isEqualTo(RepairStatus.IN_PROGRESS);
    }
}
