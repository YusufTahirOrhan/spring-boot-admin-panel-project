package com.optimaxx.management.security;

import com.optimaxx.management.domain.model.Customer;
import com.optimaxx.management.domain.model.SaleTransaction;
import com.optimaxx.management.domain.model.SaleTransactionStatus;
import com.optimaxx.management.domain.model.TransactionType;
import com.optimaxx.management.domain.model.TransactionTypeCategory;
import com.optimaxx.management.domain.repository.CustomerRepository;
import com.optimaxx.management.domain.repository.SaleTransactionRepository;
import com.optimaxx.management.domain.repository.TransactionTypeRepository;
import com.optimaxx.management.interfaces.rest.dto.CreateSaleTransactionRequest;
import com.optimaxx.management.interfaces.rest.dto.SaleTransactionResponse;
import com.optimaxx.management.interfaces.rest.dto.UpdateSaleTransactionStatusRequest;
import com.optimaxx.management.security.audit.AuditEventType;
import com.optimaxx.management.security.audit.SecurityAuditService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
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
    private final CustomerRepository customerRepository;
    private final SecurityAuditService securityAuditService;
    private final InventoryStockCoordinator inventoryStockCoordinator;

    public SalesTransactionService(SaleTransactionRepository saleTransactionRepository,
                                   TransactionTypeRepository transactionTypeRepository,
                                   CustomerRepository customerRepository,
                                   SecurityAuditService securityAuditService,
                                   InventoryStockCoordinator inventoryStockCoordinator) {
        this.saleTransactionRepository = saleTransactionRepository;
        this.transactionTypeRepository = transactionTypeRepository;
        this.customerRepository = customerRepository;
        this.securityAuditService = securityAuditService;
        this.inventoryStockCoordinator = inventoryStockCoordinator;
    }

    @Transactional
    public SaleTransactionResponse create(CreateSaleTransactionRequest request) {
        if (request == null || request.transactionTypeId() == null || request.amount() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "transactionTypeId and amount are required");
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

        Customer customer = null;
        String customerName = trimToNull(request.customerName());

        if (request.customerId() != null) {
            customer = customerRepository.findByIdAndDeletedFalse(request.customerId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Customer not found"));
            if (customer.getStoreId() != null && !StoreContext.currentStoreId().equals(customer.getStoreId())) {
                throw new ResponseStatusException(BAD_REQUEST, "Customer does not belong to current store");
            }
            if (isBlank(customerName)) {
                customerName = (customer.getFirstName() + " " + customer.getLastName()).trim();
            }
        }

        if (isBlank(customerName)) {
            throw new ResponseStatusException(BAD_REQUEST, "customerName or customerId is required");
        }

        UUID storeId = StoreContext.currentStoreId();

        SaleTransaction saleTransaction = new SaleTransaction();
        saleTransaction.setTransactionType(transactionType);
        saleTransaction.setCustomer(customer);
        saleTransaction.setCustomerName(customerName);
        saleTransaction.setAmount(request.amount());
        saleTransaction.setNotes(isBlank(request.notes()) ? null : request.notes().trim());
        saleTransaction.setOccurredAt(Instant.now());
        saleTransaction.setStoreId(storeId);
        saleTransaction.setReceiptNumber(generateReceiptNumber(storeId));
        saleTransaction.setStatus(SaleTransactionStatus.COMPLETED);
        saleTransaction.setInventoryItemId(request.inventoryItemId());
        saleTransaction.setInventoryQuantity(request.inventoryQuantity());
        saleTransaction.setStockReverted(false);
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
    public List<SaleTransactionResponse> list(Instant from, String query) {
        List<SaleTransaction> transactions = from == null
                ? saleTransactionRepository.findByDeletedFalseOrderByOccurredAtDesc()
                : saleTransactionRepository.findByOccurredAtGreaterThanEqualAndDeletedFalseOrderByOccurredAtDesc(from);

        var storeId = StoreContext.currentStoreId();
        String normalizedQuery = trimToNull(query);

        return transactions.stream()
                .filter(transaction -> (transaction.getStoreId() == null || storeId.equals(transaction.getStoreId())))
                .filter(transaction -> matchesQuery(transaction, normalizedQuery))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SaleTransactionResponse updateStatus(UUID transactionId, UpdateSaleTransactionStatusRequest request) {
        if (request == null || request.status() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "status is required");
        }

        SaleTransaction transaction = saleTransactionRepository.findByIdAndDeletedFalse(transactionId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Sale transaction not found"));

        if (transaction.getStatus() == SaleTransactionStatus.CANCELED) {
            if (request.status() == SaleTransactionStatus.CANCELED) {
                return toResponse(transaction);
            }
            throw new ResponseStatusException(BAD_REQUEST, "Canceled sale transaction cannot change status");
        }

        if (request.status() == SaleTransactionStatus.CANCELED) {
            if (transaction.getInventoryItemId() != null && transaction.getInventoryQuantity() != null && !transaction.isStockReverted()) {
                inventoryStockCoordinator.release(
                        transaction.getInventoryItemId(),
                        transaction.getInventoryQuantity(),
                        "SALE cancel rollback " + transaction.getId(),
                        "SALE_TRANSACTION_CANCEL",
                        transaction.getId(),
                        "sale:" + transaction.getId() + ":cancel-rollback"
                );
                transaction.setStockReverted(true);
            }

            transaction.setStatus(SaleTransactionStatus.CANCELED);
            securityAuditService.log(
                    AuditEventType.SALE_TRANSACTION_CANCELED,
                    null,
                    "SALE_TRANSACTION",
                    String.valueOf(transaction.getId()),
                    "{\"status\":\"CANCELED\"}"
            );
            return toResponse(transaction);
        }

        if (request.status() != SaleTransactionStatus.COMPLETED) {
            throw new ResponseStatusException(BAD_REQUEST, "Unsupported status transition");
        }

        return toResponse(transaction);
    }

    private SaleTransactionResponse toResponse(SaleTransaction transaction) {
        return new SaleTransactionResponse(
                transaction.getId(),
                transaction.getTransactionType().getId(),
                transaction.getTransactionType().getCode(),
                transaction.getReceiptNumber(),
                transaction.getStatus() == null ? SaleTransactionStatus.COMPLETED.name() : transaction.getStatus().name(),
                transaction.getCustomerId(),
                transaction.getCustomerName(),
                transaction.getAmount(),
                transaction.getNotes(),
                transaction.getOccurredAt()
        );
    }

    private String generateReceiptNumber(UUID storeId) {
        String datePart = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE);
        String prefix = "RCP-" + datePart + "-";

        int nextSequence = saleTransactionRepository
                .findTopByStoreIdAndReceiptNumberStartingWithOrderByReceiptNumberDesc(storeId, prefix)
                .map(transaction -> transaction.getReceiptNumber())
                .map(number -> number.substring(prefix.length()))
                .map(suffix -> {
                    try {
                        return Integer.parseInt(suffix);
                    } catch (NumberFormatException ex) {
                        return 0;
                    }
                })
                .orElse(0) + 1;

        for (int i = nextSequence; i < nextSequence + 10; i++) {
            String candidate = prefix + String.format(Locale.ROOT, "%04d", i);
            if (!saleTransactionRepository.existsByStoreIdAndReceiptNumberAndDeletedFalse(storeId, candidate)) {
                return candidate;
            }
        }

        throw new ResponseStatusException(BAD_REQUEST, "Could not generate unique receipt number");
    }

    private boolean matchesQuery(SaleTransaction transaction, String normalizedQuery) {
        if (normalizedQuery == null) {
            return true;
        }

        String q = normalizedQuery.toLowerCase(Locale.ROOT);
        String customerName = transaction.getCustomerName() == null ? "" : transaction.getCustomerName().toLowerCase(Locale.ROOT);
        String receiptNumber = transaction.getReceiptNumber() == null ? "" : transaction.getReceiptNumber().toLowerCase(Locale.ROOT);
        return customerName.contains(q) || receiptNumber.contains(q);
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
