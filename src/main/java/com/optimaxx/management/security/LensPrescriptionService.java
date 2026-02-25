package com.optimaxx.management.security;

import com.optimaxx.management.domain.model.Customer;
import com.optimaxx.management.domain.model.LensPrescription;
import com.optimaxx.management.domain.model.TransactionType;
import com.optimaxx.management.domain.model.TransactionTypeCategory;
import com.optimaxx.management.domain.repository.CustomerRepository;
import com.optimaxx.management.domain.repository.LensPrescriptionRepository;
import com.optimaxx.management.domain.repository.TransactionTypeRepository;
import com.optimaxx.management.interfaces.rest.dto.CreateLensPrescriptionRequest;
import com.optimaxx.management.interfaces.rest.dto.LensPrescriptionResponse;
import com.optimaxx.management.interfaces.rest.dto.UpdateLensPrescriptionRequest;
import com.optimaxx.management.security.audit.AuditEventType;
import com.optimaxx.management.security.audit.SecurityAuditService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class LensPrescriptionService {

    private final LensPrescriptionRepository lensPrescriptionRepository;
    private final CustomerRepository customerRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final SecurityAuditService securityAuditService;

    public LensPrescriptionService(LensPrescriptionRepository lensPrescriptionRepository,
                                   CustomerRepository customerRepository,
                                   TransactionTypeRepository transactionTypeRepository,
                                   SecurityAuditService securityAuditService) {
        this.lensPrescriptionRepository = lensPrescriptionRepository;
        this.customerRepository = customerRepository;
        this.transactionTypeRepository = transactionTypeRepository;
        this.securityAuditService = securityAuditService;
    }

    @Transactional
    public LensPrescriptionResponse create(CreateLensPrescriptionRequest request) {
        if (request == null || request.customerId() == null || request.transactionTypeId() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "customerId and transactionTypeId are required");
        }

        Customer customer = customerRepository.findByIdAndDeletedFalse(request.customerId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Customer not found"));

        TransactionType transactionType = transactionTypeRepository.findByIdAndDeletedFalse(request.transactionTypeId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Transaction type not found"));

        if (!transactionType.isActive()) {
            throw new ResponseStatusException(BAD_REQUEST, "Transaction type is inactive");
        }
        if (transactionType.getCategory() != TransactionTypeCategory.PRESCRIPTION) {
            throw new ResponseStatusException(BAD_REQUEST, "Transaction type category must be PRESCRIPTION");
        }

        LensPrescription prescription = new LensPrescription();
        prescription.setCustomer(customer);
        prescription.setTransactionType(transactionType);
        prescription.setRightSphere(trimToNull(request.rightSphere()));
        prescription.setLeftSphere(trimToNull(request.leftSphere()));
        prescription.setRightCylinder(trimToNull(request.rightCylinder()));
        prescription.setLeftCylinder(trimToNull(request.leftCylinder()));
        prescription.setRightAxis(trimToNull(request.rightAxis()));
        prescription.setLeftAxis(trimToNull(request.leftAxis()));
        prescription.setPd(trimToNull(request.pd()));
        prescription.setNotes(trimToNull(request.notes()));
        prescription.setRecordedAt(Instant.now());
        prescription.setStoreId(UUID.randomUUID());
        prescription.setDeleted(false);

        LensPrescription saved = lensPrescriptionRepository.save(prescription);

        securityAuditService.log(
                AuditEventType.PRESCRIPTION_CREATED,
                null,
                "LENS_PRESCRIPTION",
                String.valueOf(saved.getId()),
                "{\"customerId\":\"" + saved.getCustomer().getId() + "\"}"
        );

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<LensPrescriptionResponse> list() {
        return lensPrescriptionRepository.findByDeletedFalseOrderByRecordedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LensPrescriptionResponse get(UUID id) {
        LensPrescription prescription = lensPrescriptionRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Prescription not found"));
        return toResponse(prescription);
    }

    @Transactional
    public LensPrescriptionResponse update(UUID id, UpdateLensPrescriptionRequest request) {
        if (request == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Update payload is required");
        }

        LensPrescription prescription = lensPrescriptionRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Prescription not found"));

        if (request.rightSphere() != null) {
            prescription.setRightSphere(trimToNull(request.rightSphere()));
        }
        if (request.leftSphere() != null) {
            prescription.setLeftSphere(trimToNull(request.leftSphere()));
        }
        if (request.rightCylinder() != null) {
            prescription.setRightCylinder(trimToNull(request.rightCylinder()));
        }
        if (request.leftCylinder() != null) {
            prescription.setLeftCylinder(trimToNull(request.leftCylinder()));
        }
        if (request.rightAxis() != null) {
            prescription.setRightAxis(trimToNull(request.rightAxis()));
        }
        if (request.leftAxis() != null) {
            prescription.setLeftAxis(trimToNull(request.leftAxis()));
        }
        if (request.pd() != null) {
            prescription.setPd(trimToNull(request.pd()));
        }
        if (request.notes() != null) {
            prescription.setNotes(trimToNull(request.notes()));
        }

        securityAuditService.log(
                AuditEventType.PRESCRIPTION_UPDATED,
                null,
                "LENS_PRESCRIPTION",
                String.valueOf(prescription.getId()),
                "{\"updated\":true}"
        );

        return toResponse(prescription);
    }

    private LensPrescriptionResponse toResponse(LensPrescription prescription) {
        return new LensPrescriptionResponse(
                prescription.getId(),
                prescription.getCustomer().getId(),
                prescription.getTransactionType().getId(),
                prescription.getRightSphere(),
                prescription.getLeftSphere(),
                prescription.getRightCylinder(),
                prescription.getLeftCylinder(),
                prescription.getRightAxis(),
                prescription.getLeftAxis(),
                prescription.getPd(),
                prescription.getNotes(),
                prescription.getRecordedAt()
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
