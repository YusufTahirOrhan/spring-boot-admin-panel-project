package com.optimaxx.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.optimaxx.management.domain.model.Customer;
import com.optimaxx.management.domain.repository.CustomerRepository;
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
        SecurityAuditService securityAuditService = Mockito.mock(SecurityAuditService.class);

        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerService customerService = new CustomerService(customerRepository, securityAuditService);
        var response = customerService.create(new CreateCustomerRequest("Yusuf", "Orhan", "555", "y@x.com", "vip"));

        assertThat(response.firstName()).isEqualTo("Yusuf");
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void shouldRejectInvalidCustomerPayload() {
        CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
        SecurityAuditService securityAuditService = Mockito.mock(SecurityAuditService.class);

        CustomerService customerService = new CustomerService(customerRepository, securityAuditService);

        assertThatThrownBy(() -> customerService.create(new CreateCustomerRequest("", "", null, null, null)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldListCustomers() {
        CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
        SecurityAuditService securityAuditService = Mockito.mock(SecurityAuditService.class);

        Customer customer = new Customer();
        customer.setFirstName("Yusuf");
        customer.setLastName("Orhan");

        when(customerRepository.findByDeletedFalseOrderByCreatedAtDesc()).thenReturn(List.of(customer));

        CustomerService customerService = new CustomerService(customerRepository, securityAuditService);

        assertThat(customerService.list(null)).hasSize(1);
    }

    @Test
    void shouldUpdateCustomer() {
        CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
        SecurityAuditService securityAuditService = Mockito.mock(SecurityAuditService.class);

        UUID id = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setFirstName("Yusuf");
        customer.setLastName("Orhan");

        when(customerRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(customer));

        CustomerService customerService = new CustomerService(customerRepository, securityAuditService);
        var response = customerService.update(id, new UpdateCustomerRequest("Yusuf T.", "Orhan", null, null, "updated"));

        assertThat(response.firstName()).isEqualTo("Yusuf T.");
    }
}
