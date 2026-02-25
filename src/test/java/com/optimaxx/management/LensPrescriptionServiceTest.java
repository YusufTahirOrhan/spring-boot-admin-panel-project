package com.optimaxx.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.optimaxx.management.domain.model.Customer;
import com.optimaxx.management.domain.model.LensPrescription;
import com.optimaxx.management.domain.model.TransactionType;
import com.optimaxx.management.domain.model.TransactionTypeCategory;
import com.optimaxx.management.domain.repository.CustomerRepository;
import com.optimaxx.management.domain.repository.LensPrescriptionRepository;
import com.optimaxx.management.domain.repository.TransactionTypeRepository;
import com.optimaxx.management.interfaces.rest.dto.CreateLensPrescriptionRequest;
import com.optimaxx.management.interfaces.rest.dto.UpdateLensPrescriptionRequest;
import com.optimaxx.management.security.LensPrescriptionService;
import com.optimaxx.management.security.audit.SecurityAuditService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

class LensPrescriptionServiceTest {

    @Test
    void shouldCreatePrescription() {
        LensPrescriptionRepository prescriptionRepository = Mockito.mock(LensPrescriptionRepository.class);
        CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
        TransactionTypeRepository transactionTypeRepository = Mockito.mock(TransactionTypeRepository.class);
        SecurityAuditService securityAuditService = Mockito.mock(SecurityAuditService.class);

        UUID customerId = UUID.randomUUID();
        UUID transactionTypeId = UUID.randomUUID();

        Customer customer = new Customer();
        TransactionType transactionType = new TransactionType();
        transactionType.setCategory(TransactionTypeCategory.PRESCRIPTION);
        transactionType.setActive(true);

        when(customerRepository.findByIdAndDeletedFalse(customerId)).thenReturn(Optional.of(customer));
        when(transactionTypeRepository.findByIdAndDeletedFalse(transactionTypeId)).thenReturn(Optional.of(transactionType));
        when(prescriptionRepository.save(any(LensPrescription.class))).thenAnswer(invocation -> {
            LensPrescription value = invocation.getArgument(0);
            value.setStoreId(UUID.randomUUID());
            return value;
        });

        LensPrescriptionService service = new LensPrescriptionService(
                prescriptionRepository,
                customerRepository,
                transactionTypeRepository,
                securityAuditService
        );

        var response = service.create(new CreateLensPrescriptionRequest(
                customerId,
                transactionTypeId,
                "-1.25",
                "-1.00",
                "-0.50",
                "-0.25",
                "90",
                "95",
                "62",
                "daily use"
        ));

        assertThat(response.rightSphere()).isEqualTo("-1.25");
        verify(prescriptionRepository).save(any(LensPrescription.class));
    }

    @Test
    void shouldRejectNonPrescriptionTransactionType() {
        LensPrescriptionRepository prescriptionRepository = Mockito.mock(LensPrescriptionRepository.class);
        CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
        TransactionTypeRepository transactionTypeRepository = Mockito.mock(TransactionTypeRepository.class);
        SecurityAuditService securityAuditService = Mockito.mock(SecurityAuditService.class);

        UUID customerId = UUID.randomUUID();
        UUID transactionTypeId = UUID.randomUUID();

        Customer customer = new Customer();
        TransactionType transactionType = new TransactionType();
        transactionType.setCategory(TransactionTypeCategory.SALE);
        transactionType.setActive(true);

        when(customerRepository.findByIdAndDeletedFalse(customerId)).thenReturn(Optional.of(customer));
        when(transactionTypeRepository.findByIdAndDeletedFalse(transactionTypeId)).thenReturn(Optional.of(transactionType));

        LensPrescriptionService service = new LensPrescriptionService(
                prescriptionRepository,
                customerRepository,
                transactionTypeRepository,
                securityAuditService
        );

        assertThatThrownBy(() -> service.create(new CreateLensPrescriptionRequest(
                customerId,
                transactionTypeId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ))).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldListAndUpdatePrescription() {
        LensPrescriptionRepository prescriptionRepository = Mockito.mock(LensPrescriptionRepository.class);
        CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
        TransactionTypeRepository transactionTypeRepository = Mockito.mock(TransactionTypeRepository.class);
        SecurityAuditService securityAuditService = Mockito.mock(SecurityAuditService.class);

        UUID prescriptionId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID transactionTypeId = UUID.randomUUID();

        Customer customer = new Customer();
        customer.setStoreId(UUID.randomUUID());

        TransactionType transactionType = new TransactionType();
        transactionType.setStoreId(UUID.randomUUID());

        LensPrescription prescription = new LensPrescription();
        prescription.setStoreId(UUID.randomUUID());
        prescription.setCustomer(customer);
        prescription.setTransactionType(transactionType);
        prescription.setRecordedAt(java.time.Instant.now());
        prescription.setRightSphere("-1.00");

        when(prescriptionRepository.findByDeletedFalseOrderByRecordedAtDesc()).thenReturn(List.of(prescription));
        when(prescriptionRepository.findByIdAndDeletedFalse(prescriptionId)).thenReturn(Optional.of(prescription));

        LensPrescriptionService service = new LensPrescriptionService(
                prescriptionRepository,
                customerRepository,
                transactionTypeRepository,
                securityAuditService
        );

        assertThat(service.list()).hasSize(1);

        var updated = service.update(prescriptionId, new UpdateLensPrescriptionRequest(
                "-1.50", null, null, null, null, null, null, "updated"
        ));

        assertThat(updated.rightSphere()).isEqualTo("-1.50");
        assertThat(updated.notes()).isEqualTo("updated");
    }
}
