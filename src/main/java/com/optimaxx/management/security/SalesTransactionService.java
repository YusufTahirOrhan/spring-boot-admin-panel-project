package com.optimaxx.management.security;

import com.optimaxx.management.domain.model.SaleTransaction;
import com.optimaxx.management.domain.model.TransactionType;
import com.optimaxx.management.domain.model.TransactionTypeCategory;
import com.optimaxx.management.domain.repository.SaleTransactionRepository;
import com.optimaxx.management.domain.repository.TransactionTypeRepository;
import com.optimaxx.management.interfaces.rest.dto.CreateSaleTransactionRequest;
import com.optimaxx.management.interfaces.rest.dto.SaleTransactionResponse;
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
public class SalesTransactionService {

    private final SaleTransactionRepository saleTransactionRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final SecurityAuditService securityAuditService;
    private final InventoryStockCoordinator inventoryStockCoordinator;

    public SalesTransactionService(SaleTransactionRepository saleTransactionRepository,
                                   TransactionTypeRepository transactionTypeRepository,
                                   SecurityAuditService securityAuditService,
                                   InventoryStockCoordinator inventoryStockCoordinator) {
        this.saleTransactionRepository = saleTransactionRepository;
        this.transactionTypeRepository = transactionTypeRepository;
        this.securityAuditService = securityAuditService;
        this.inventoryStockCoordinator = inventoryStockCoordinator;
    }

    @Transactional
    public SaleTransactionResponse create(CreateSaleTransactionRequest request) {
        if (request == null || request.transactionTypeId() == null || isBlank(request.customerName()) || request.amount() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "transactionTypeId, customerName and amount are required");
        }
        if (request.amount().signum() <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "amount must be greater than zero");
        }

        TransactionType transactionType = transactionTypeRepository.findByIdAndDeletedFalse(request.transactionTypeId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Transaction type not found"));

        if (!transactionType.isActive()) {
            throw new ResponseStatusException(BAD_REQUEST, "Transaction type is inactive");
        }
        if (transactionType.getCategory() != TransactionTypeCategory.SALE) {
            throw new ResponseStatusException(BAD_REQUEST, "Transaction type category must be SALE");
        }

        if ((request.inventoryItemId() == null) != (request.inventoryQuantity() == null)) {
            throw new ResponseStatusException(BAD_REQUEST, "inventoryItemId and inventoryQuantity must be provided together");
        }

        SaleTransaction saleTransaction = new SaleTransaction();
        saleTransaction.setTransactionType(transactionType);
        saleTransaction.setCustomerName(request.customerName().trim());
        saleTransaction.setAmount(request.amount());
        saleTransaction.setNotes(isBlank(request.notes()) ? null : request.notes().trim());
        saleTransaction.setOccurredAt(Instant.now());
        saleTransaction.setStoreId(UUID.randomUUID());
        saleTransaction.setDeleted(false);

        SaleTransaction saved = saleTransactionRepository.save(saleTransaction);

        if (request.inventoryItemId() != null) {
            var item = inventoryStockCoordinator.consume(
                    request.inventoryItemId(),
                    request.inventoryQuantity(),
                    "SALE transaction " + saved.getId(),
                    "SALE_TRANSACTION",
                    saved.getId(),
                    "sale:" + saved.getId() + ":consume"
            );
            securityAuditService.log(
                    AuditEventType.SALE_STOCK_DEDUCTED,
                    null,
                    "INVENTORY",
                    item.getSku(),
                    "{\"saleTransactionId\":\"" + saved.getId() + "\",\"quantity\":" + request.inventoryQuantity() + "}"
            );
        }

        securityAuditService.log(
                AuditEventType.SALE_TRANSACTION_CREATED,
                null,
                "SALE_TRANSACTION",
                String.valueOf(saved.getId()),
                "{\"transactionType\":\"" + transactionType.getCode() + "\",\"amount\":" + request.amount() + "}"
        );
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SaleTransactionResponse> list(Instant from) {
        List<SaleTransaction> transactions = from == null
                ? saleTransactionRepository.findByDeletedFalseOrderByOccurredAtDesc()
                : saleTransactionRepository.findByOccurredAtGreaterThanEqualAndDeletedFalseOrderByOccurredAtDesc(from);

        return transactions.stream().map(this::toResponse).toList();
    }

    private SaleTransactionResponse toResponse(SaleTransaction transaction) {
        return new SaleTransactionResponse(
                transaction.getId(),
                transaction.getTransactionType().getId(),
                transaction.getTransactionType().getCode(),
                transaction.getCustomerName(),
                transaction.getAmount(),
                transaction.getNotes(),
                transaction.getOccurredAt()
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
