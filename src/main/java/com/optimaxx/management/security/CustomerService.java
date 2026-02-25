package com.optimaxx.management.security;

import com.optimaxx.management.domain.model.Customer;
import com.optimaxx.management.domain.repository.CustomerRepository;
import com.optimaxx.management.domain.repository.LensPrescriptionRepository;
import com.optimaxx.management.domain.repository.RepairOrderRepository;
import com.optimaxx.management.domain.repository.SaleTransactionRepository;
import com.optimaxx.management.interfaces.rest.dto.CreateCustomerRequest;
import com.optimaxx.management.interfaces.rest.dto.CustomerResponse;
import com.optimaxx.management.interfaces.rest.dto.UpdateCustomerRequest;
import com.optimaxx.management.security.audit.AuditEventType;
import com.optimaxx.management.security.audit.SecurityAuditService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final SaleTransactionRepository saleTransactionRepository;
    private final RepairOrderRepository repairOrderRepository;
    private final LensPrescriptionRepository lensPrescriptionRepository;
    private final SecurityAuditService securityAuditService;

    public CustomerService(CustomerRepository customerRepository,
                           SaleTransactionRepository saleTransactionRepository,
                           RepairOrderRepository repairOrderRepository,
                           LensPrescriptionRepository lensPrescriptionRepository,
                           SecurityAuditService securityAuditService) {
        this.customerRepository = customerRepository;
        this.saleTransactionRepository = saleTransactionRepository;
        this.repairOrderRepository = repairOrderRepository;
        this.lensPrescriptionRepository = lensPrescriptionRepository;
        this.securityAuditService = securityAuditService;
    }

    @Transactional
    public CustomerResponse create(CreateCustomerRequest request) {
        if (request == null || isBlank(request.firstName()) || isBlank(request.lastName())) {
            throw new ResponseStatusException(BAD_REQUEST, "firstName and lastName are required");
        }

        Customer customer = new Customer();
        customer.setFirstName(request.firstName().trim());
        customer.setLastName(request.lastName().trim());
        customer.setPhone(trimToNull(request.phone()));
        customer.setEmail(trimToNull(request.email()));
        customer.setNotes(trimToNull(request.notes()));
        customer.setStoreId(StoreContext.currentStoreId());
        customer.setDeleted(false);

        Customer saved = customerRepository.save(customer);
        securityAuditService.log(AuditEventType.CUSTOMER_CREATED, null, "CUSTOMER", String.valueOf(saved.getId()), "{\"name\":\"" + saved.getFirstName() + " " + saved.getLastName() + "\"}");
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(UUID id) {
        Customer customer = customerRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Customer not found"));
        return toResponse(customer);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> list(String query) {
        List<Customer> customers;
        if (isBlank(query)) {
            customers = customerRepository.findByDeletedFalseOrderByCreatedAtDesc();
        } else {
            String normalized = query.trim();
            customers = customerRepository
                    .findByDeletedFalseAndFirstNameContainingIgnoreCaseOrDeletedFalseAndLastNameContainingIgnoreCaseOrderByCreatedAtDesc(normalized, normalized);
        }
        var storeId = StoreContext.currentStoreId();
        return customers.stream()
                .filter(customer -> (customer.getStoreId() == null || storeId.equals(customer.getStoreId())))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CustomerResponse update(UUID id, UpdateCustomerRequest request) {
        if (request == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Update payload is required");
        }

        Customer customer = customerRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Customer not found"));

        if (!isBlank(request.firstName())) {
            customer.setFirstName(request.firstName().trim());
        }
        if (!isBlank(request.lastName())) {
            customer.setLastName(request.lastName().trim());
        }
        if (request.phone() != null) {
            customer.setPhone(trimToNull(request.phone()));
        }
        if (request.email() != null) {
            customer.setEmail(trimToNull(request.email()));
        }
        if (request.notes() != null) {
            customer.setNotes(trimToNull(request.notes()));
        }

        securityAuditService.log(AuditEventType.CUSTOMER_UPDATED, null, "CUSTOMER", String.valueOf(customer.getId()), "{\"updated\":true}");
        return toResponse(customer);
    }

    @Transactional
    public void softDelete(UUID id) {
        Customer customer = customerRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Customer not found"));

        boolean hasSaleLinks = saleTransactionRepository.existsByCustomerAndDeletedFalse(customer);
        boolean hasRepairLinks = repairOrderRepository.existsByCustomerAndDeletedFalse(customer);
        boolean hasPrescriptionLinks = lensPrescriptionRepository.existsByCustomerAndDeletedFalse(customer);

        if (hasSaleLinks || hasRepairLinks || hasPrescriptionLinks) {
            throw new ResponseStatusException(CONFLICT, "Customer has active linked records and cannot be deleted");
        }

        customer.setDeleted(true);
        customer.setDeletedAt(Instant.now());

        securityAuditService.log(
                AuditEventType.CUSTOMER_DELETED,
                null,
                "CUSTOMER",
                String.valueOf(customer.getId()),
                "{\"deleted\":true}"
        );
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getPhone(),
                customer.getEmail(),
                customer.getNotes()
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
