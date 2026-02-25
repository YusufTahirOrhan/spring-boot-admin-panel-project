package com.optimaxx.management.security;

import com.optimaxx.management.domain.model.TransactionType;
import com.optimaxx.management.domain.model.TransactionTypeCategory;
import com.optimaxx.management.domain.repository.TransactionTypeRepository;
import com.optimaxx.management.interfaces.rest.dto.AdminCreateTransactionTypeRequest;
import com.optimaxx.management.interfaces.rest.dto.AdminUpdateTransactionTypeRequest;
import com.optimaxx.management.interfaces.rest.dto.TransactionTypeResponse;
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
public class TransactionTypeManagementService {

    private final TransactionTypeRepository transactionTypeRepository;
    private final SecurityAuditService securityAuditService;
    private final TransactionTypeMetadataValidator transactionTypeMetadataValidator;

    public TransactionTypeManagementService(TransactionTypeRepository transactionTypeRepository,
                                            SecurityAuditService securityAuditService,
                                            TransactionTypeMetadataValidator transactionTypeMetadataValidator) {
        this.transactionTypeRepository = transactionTypeRepository;
        this.securityAuditService = securityAuditService;
        this.transactionTypeMetadataValidator = transactionTypeMetadataValidator;
    }

    @Transactional
    public TransactionTypeResponse create(AdminCreateTransactionTypeRequest request) {
        if (request == null || isBlank(request.code()) || isBlank(request.name())) {
            throw new ResponseStatusException(BAD_REQUEST, "Code and name are required");
        }

        String normalizedCode = request.code().trim().toUpperCase();
        if (transactionTypeRepository.existsByCodeAndDeletedFalse(normalizedCode)) {
            throw new ResponseStatusException(BAD_REQUEST, "Transaction type code already exists");
        }

        TransactionTypeCategory category = request.category() == null ? TransactionTypeCategory.SALE : request.category();
        String metadataJson = trimToNull(request.metadataJson());
        transactionTypeMetadataValidator.validate(category, metadataJson);

        TransactionType transactionType = new TransactionType();
        transactionType.setCode(normalizedCode);
        transactionType.setName(request.name().trim());
        transactionType.setActive(request.active() == null || request.active());
        transactionType.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        transactionType.setCategory(category);
        transactionType.setMetadataJson(metadataJson);
        transactionType.setStoreId(StoreContext.currentStoreId());
        transactionType.setDeleted(false);

        TransactionType saved = transactionTypeRepository.save(transactionType);
        securityAuditService.log(AuditEventType.TRANSACTION_TYPE_CREATED, null, "TRANSACTION_TYPE", saved.getCode(), "{\"name\":\"" + saved.getName() + "\"}");
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TransactionTypeResponse> listAdmin() {
        var storeId = StoreContext.currentStoreId();
        return transactionTypeRepository.findByDeletedFalseOrderBySortOrderAscNameAsc().stream()
                .filter(type -> (type.getStoreId() == null || storeId.equals(type.getStoreId())))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransactionTypeResponse> listSales() {
        var storeId = StoreContext.currentStoreId();
        return transactionTypeRepository.findByActiveTrueAndDeletedFalseOrderBySortOrderAscNameAsc().stream()
                .filter(type -> (type.getStoreId() == null || storeId.equals(type.getStoreId())))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TransactionTypeResponse update(UUID transactionTypeId, AdminUpdateTransactionTypeRequest request) {
        if (request == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Update payload is required");
        }

        TransactionType transactionType = transactionTypeRepository.findByIdAndDeletedFalse(transactionTypeId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Transaction type not found"));

        if (!isBlank(request.name())) {
            transactionType.setName(request.name().trim());
        }
        if (request.active() != null) {
            transactionType.setActive(request.active());
        }
        if (request.sortOrder() != null) {
            transactionType.setSortOrder(request.sortOrder());
        }

        TransactionTypeCategory nextCategory = request.category() == null
                ? transactionType.getCategory()
                : request.category();
        String nextMetadata = request.metadataJson() == null
                ? transactionType.getMetadataJson()
                : trimToNull(request.metadataJson());

        transactionTypeMetadataValidator.validate(nextCategory, nextMetadata);
        transactionType.setCategory(nextCategory);
        transactionType.setMetadataJson(nextMetadata);

        securityAuditService.log(AuditEventType.TRANSACTION_TYPE_UPDATED, null, "TRANSACTION_TYPE", transactionType.getCode(), "{\"updated\":true}");
        return toResponse(transactionType);
    }

    @Transactional
    public void softDelete(UUID transactionTypeId) {
        TransactionType transactionType = transactionTypeRepository.findByIdAndDeletedFalse(transactionTypeId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Transaction type not found"));

        transactionType.setDeleted(true);
        transactionType.setActive(false);
        transactionType.setDeletedAt(Instant.now());
        transactionType.setDeletedBy(null);

        securityAuditService.log(AuditEventType.TRANSACTION_TYPE_DELETED, null, "TRANSACTION_TYPE", transactionType.getCode(), "{\"deleted\":true}");
    }

    private TransactionTypeResponse toResponse(TransactionType transactionType) {
        return new TransactionTypeResponse(
                transactionType.getId(),
                transactionType.getCode(),
                transactionType.getName(),
                transactionType.isActive(),
                transactionType.getSortOrder(),
                transactionType.getCategory(),
                transactionType.getMetadataJson()
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
