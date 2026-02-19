package com.optimaxx.management.security;

import com.optimaxx.management.domain.model.Customer;
import com.optimaxx.management.domain.model.RepairOrder;
import com.optimaxx.management.domain.model.RepairStatus;
import com.optimaxx.management.domain.model.TransactionType;
import com.optimaxx.management.domain.model.TransactionTypeCategory;
import com.optimaxx.management.domain.repository.CustomerRepository;
import com.optimaxx.management.domain.repository.RepairOrderRepository;
import com.optimaxx.management.domain.repository.TransactionTypeRepository;
import com.optimaxx.management.interfaces.rest.dto.CreateRepairOrderRequest;
import com.optimaxx.management.interfaces.rest.dto.RepairOrderResponse;
import com.optimaxx.management.interfaces.rest.dto.UpdateRepairStatusRequest;
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
public class RepairOrderService {

    private final RepairOrderRepository repairOrderRepository;
    private final CustomerRepository customerRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final SecurityAuditService securityAuditService;

    public RepairOrderService(RepairOrderRepository repairOrderRepository,
                              CustomerRepository customerRepository,
                              TransactionTypeRepository transactionTypeRepository,
                              SecurityAuditService securityAuditService) {
        this.repairOrderRepository = repairOrderRepository;
        this.customerRepository = customerRepository;
        this.transactionTypeRepository = transactionTypeRepository;
        this.securityAuditService = securityAuditService;
    }

    @Transactional
    public RepairOrderResponse create(CreateRepairOrderRequest request) {
        if (request == null || request.customerId() == null || request.transactionTypeId() == null || isBlank(request.title())) {
            throw new ResponseStatusException(BAD_REQUEST, "customerId, transactionTypeId and title are required");
        }

        Customer customer = customerRepository.findByIdAndDeletedFalse(request.customerId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Customer not found"));

        TransactionType transactionType = transactionTypeRepository.findByIdAndDeletedFalse(request.transactionTypeId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Transaction type not found"));

        if (!transactionType.isActive()) {
            throw new ResponseStatusException(BAD_REQUEST, "Transaction type is inactive");
        }
        if (transactionType.getCategory() != TransactionTypeCategory.REPAIR) {
            throw new ResponseStatusException(BAD_REQUEST, "Transaction type category must be REPAIR");
        }

        RepairOrder order = new RepairOrder();
        order.setCustomer(customer);
        order.setTransactionType(transactionType);
        order.setTitle(request.title().trim());
        order.setDescription(trimToNull(request.description()));
        order.setStatus(RepairStatus.RECEIVED);
        order.setReceivedAt(Instant.now());
        order.setStoreId(UUID.randomUUID());
        order.setDeleted(false);

        RepairOrder saved = repairOrderRepository.save(order);

        securityAuditService.log(
                AuditEventType.REPAIR_ORDER_CREATED,
                null,
                "REPAIR_ORDER",
                String.valueOf(saved.getId()),
                "{\"status\":\"RECEIVED\"}"
        );

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<RepairOrderResponse> list() {
        return repairOrderRepository.findByDeletedFalseOrderByReceivedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RepairOrderResponse updateStatus(UUID repairOrderId, UpdateRepairStatusRequest request) {
        if (request == null || request.status() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "status is required");
        }

        RepairOrder order = repairOrderRepository.findByIdAndDeletedFalse(repairOrderId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Repair order not found"));

        order.setStatus(request.status());

        securityAuditService.log(
                AuditEventType.REPAIR_STATUS_UPDATED,
                null,
                "REPAIR_ORDER",
                String.valueOf(order.getId()),
                "{\"status\":\"" + request.status().name() + "\"}"
        );

        return toResponse(order);
    }

    private RepairOrderResponse toResponse(RepairOrder order) {
        return new RepairOrderResponse(
                order.getId(),
                order.getCustomer().getId(),
                order.getTransactionType().getId(),
                order.getTitle(),
                order.getDescription(),
                order.getStatus(),
                order.getReceivedAt()
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
