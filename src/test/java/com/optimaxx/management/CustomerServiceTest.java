package com.optimaxx.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.optimaxx.management.domain.model.Customer;
import com.optimaxx.management.domain.repository.CustomerRepository;
import com.optimaxx.management.domain.repository.LensPrescriptionRepository;
import com.optimaxx.management.domain.repository.RepairOrderRepository;
import com.optimaxx.management.domain.repository.SaleTransactionRepository;
import com.optimaxx.management.interfaces.rest.dto.CreateCustomerRequest;
import com.optimaxx.management.interfaces.rest.dto.UpdateCustomerRequest;
import com.optimaxx.management.security.CustomerService;
import com.optimaxx.management.security.audit.SecurityAuditService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

class CustomerServiceTest {

    @Test
    void shouldCreateCustomer() {
        CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
        SaleTransactionRepository saleTransactionRepository = Mockito.mock(SaleTransactionRepository.class);
        RepairOrderRepository repairOrderRepository = Mockito.mock(RepairOrderRepository.class);
        LensPrescriptionRepository lensPrescriptionRepository = Mockito.mock(LensPrescriptionRepository.class);
        SecurityAuditService securityAuditService = Mockito.mock(SecurityAuditService.class);

        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerService customerService = new CustomerService(
                customerRepository,
                saleTransactionRepository,
                repairOrderRepository,
                lensPrescriptionRepository,
                securityAuditService
        );
        var response = customerService.create(new CreateCustomerRequest("Yusuf", "Orhan", "555", "y@x.com", "vip"));

        assertThat(response.firstName()).isEqualTo("Yusuf");
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void shouldRejectInvalidCustomerPayload() {
        CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
        SaleTransactionRepository saleTransactionRepository = Mockito.mock(SaleTransactionRepository.class);
        RepairOrderRepository repairOrderRepository = Mockito.mock(RepairOrderRepository.class);
        LensPrescriptionRepository lensPrescriptionRepository = Mockito.mock(LensPrescriptionRepository.class);
        SecurityAuditService securityAuditService = Mockito.mock(SecurityAuditService.class);

        CustomerService customerService = new CustomerService(
                customerRepository,
                saleTransactionRepository,
                repairOrderRepository,
                lensPrescriptionRepository,
                securityAuditService
        );

        assertThatThrownBy(() -> customerService.create(new CreateCustomerRequest("", "", null, null, null)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldListCustomers() {
        CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
        SaleTransactionRepository saleTransactionRepository = Mockito.mock(SaleTransactionRepository.class);
        RepairOrderRepository repairOrderRepository = Mockito.mock(RepairOrderRepository.class);
        LensPrescriptionRepository lensPrescriptionRepository = Mockito.mock(LensPrescriptionRepository.class);
        SecurityAuditService securityAuditService = Mockito.mock(SecurityAuditService.class);

        Customer customer = new Customer();
        customer.setFirstName("Yusuf");
        customer.setLastName("Orhan");

        when(customerRepository.findByDeletedFalseOrderByCreatedAtDesc()).thenReturn(List.of(customer));

        CustomerService customerService = new CustomerService(
                customerRepository,
                saleTransactionRepository,
                repairOrderRepository,
                lensPrescriptionRepository,
                securityAuditService
        );

        assertThat(customerService.list(null)).hasSize(1);
    }

    @Test
    void shouldSoftDeleteCustomer() {
        CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
        SaleTransactionRepository saleTransactionRepository = Mockito.mock(SaleTransactionRepository.class);
        RepairOrderRepository repairOrderRepository = Mockito.mock(RepairOrderRepository.class);
        LensPrescriptionRepository lensPrescriptionRepository = Mockito.mock(LensPrescriptionRepository.class);
        SecurityAuditService securityAuditService = Mockito.mock(SecurityAuditService.class);

        UUID id = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setDeleted(false);

        when(customerRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(customer));
        when(saleTransactionRepository.existsByCustomerAndDeletedFalse(customer)).thenReturn(false);
        when(repairOrderRepository.existsByCustomerAndDeletedFalse(customer)).thenReturn(false);
        when(lensPrescriptionRepository.existsByCustomerAndDeletedFalse(customer)).thenReturn(false);

        CustomerService customerService = new CustomerService(
                customerRepository,
                saleTransactionRepository,
                repairOrderRepository,
                lensPrescriptionRepository,
                securityAuditService
        );
        customerService.softDelete(id);

        assertThat(customer.isDeleted()).isTrue();
        assertThat(customer.getDeletedAt()).isNotNull();
    }

    @Test
    void shouldRejectSoftDeleteWhenCustomerHasLinkedRecords() {
        CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
        SaleTransactionRepository saleTransactionRepository = Mockito.mock(SaleTransactionRepository.class);
        RepairOrderRepository repairOrderRepository = Mockito.mock(RepairOrderRepository.class);
        LensPrescriptionRepository lensPrescriptionRepository = Mockito.mock(LensPrescriptionRepository.class);
        SecurityAuditService securityAuditService = Mockito.mock(SecurityAuditService.class);

        UUID id = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setDeleted(false);

        when(customerRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(customer));
        when(saleTransactionRepository.existsByCustomerAndDeletedFalse(customer)).thenReturn(true);

        CustomerService customerService = new CustomerService(
                customerRepository,
                saleTransactionRepository,
                repairOrderRepository,
                lensPrescriptionRepository,
                securityAuditService
        );

        assertThatThrownBy(() -> customerService.softDelete(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");
    }

    @Test
    void shouldUpdateCustomer() {
        CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
        SaleTransactionRepository saleTransactionRepository = Mockito.mock(SaleTransactionRepository.class);
        RepairOrderRepository repairOrderRepository = Mockito.mock(RepairOrderRepository.class);
        LensPrescriptionRepository lensPrescriptionRepository = Mockito.mock(LensPrescriptionRepository.class);
        SecurityAuditService securityAuditService = Mockito.mock(SecurityAuditService.class);

        UUID id = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setFirstName("Yusuf");
        customer.setLastName("Orhan");

        when(customerRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(customer));

        CustomerService customerService = new CustomerService(
                customerRepository,
                saleTransactionRepository,
                repairOrderRepository,
                lensPrescriptionRepository,
                securityAuditService
        );
        var response = customerService.update(id, new UpdateCustomerRequest("Yusuf T.", "Orhan", null, null, "updated"));

        assertThat(response.firstName()).isEqualTo("Yusuf T.");
    }
}
