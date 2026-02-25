package com.optimaxx.management.security;

import com.optimaxx.management.domain.model.Customer;
import com.optimaxx.management.domain.model.SalePaymentMethod;
import com.optimaxx.management.domain.model.SaleTransaction;
import com.optimaxx.management.domain.model.SaleTransactionStatus;
import com.optimaxx.management.domain.model.ActivityLog;
import com.optimaxx.management.domain.model.TransactionType;
import com.optimaxx.management.domain.model.TransactionTypeCategory;
import com.optimaxx.management.domain.repository.ActivityLogRepository;
import com.optimaxx.management.domain.repository.CustomerRepository;
import com.optimaxx.management.domain.repository.SaleTransactionRepository;
import com.optimaxx.management.domain.repository.TransactionTypeRepository;
import com.optimaxx.management.interfaces.rest.dto.CreateSaleTransactionRequest;
import com.optimaxx.management.interfaces.rest.dto.RefundSaleTransactionRequest;
import com.optimaxx.management.interfaces.rest.dto.SalePaymentMethodSummaryResponse;
import com.optimaxx.management.interfaces.rest.dto.SaleTransactionDetailResponse;
import com.optimaxx.management.interfaces.rest.dto.SaleTransactionResponse;
import com.optimaxx.management.interfaces.rest.dto.SaleTransactionSummaryResponse;
import com.optimaxx.management.interfaces.rest.dto.SaleTransactionTimelineEventResponse;
import com.optimaxx.management.interfaces.rest.dto.UpdateSaleTransactionStatusRequest;
import com.optimaxx.management.security.audit.AuditEventType;
import com.optimaxx.management.security.audit.SecurityAuditService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
    private final ActivityLogRepository activityLogRepository;
    private final SecurityAuditService securityAuditService;
    private final InventoryStockCoordinator inventoryStockCoordinator;

    public SalesTransactionService(SaleTransactionRepository saleTransactionRepository,
                                   TransactionTypeRepository transactionTypeRepository,
                                   CustomerRepository customerRepository,
                                   ActivityLogRepository activityLogRepository,
                                   SecurityAuditService securityAuditService,
                                   InventoryStockCoordinator inventoryStockCoordinator) {
        this.saleTransactionRepository = saleTransactionRepository;
        this.transactionTypeRepository = transactionTypeRepository;
        this.customerRepository = customerRepository;
        this.activityLogRepository = activityLogRepository;
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
        saleTransaction.setPaymentMethod(parsePaymentMethodOrDefault(request.paymentMethod()));
        saleTransaction.setPaymentReference(trimToNull(request.paymentReference()));
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
                "{\"transactionType\":\"" + transactionType.getCode() + "\",\"amount\":" + request.amount() + ",\"paymentMethod\":\"" + saleTransaction.getPaymentMethod().name() + "\"}"
        );
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<SaleTransactionResponse> list(Instant from,
                                              Instant to,
                                              String query,
                                              String paymentMethod,
                                              int page,
                                              int size,
                                              String sort) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(BAD_REQUEST, "from cannot be after to");
        }
        if (page < 0 || size <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "page must be >= 0 and size must be > 0");
        }

        List<SaleTransaction> transactions = from == null
                ? saleTransactionRepository.findByDeletedFalseOrderByOccurredAtDesc()
                : saleTransactionRepository.findByOccurredAtGreaterThanEqualAndDeletedFalseOrderByOccurredAtDesc(from);

        var storeId = StoreContext.currentStoreId();
        String normalizedQuery = trimToNull(query);
        SalePaymentMethod paymentMethodFilter = parsePaymentMethod(paymentMethod);

        List<SaleTransactionResponse> filtered = transactions.stream()
                .filter(transaction -> (transaction.getStoreId() == null || storeId.equals(transaction.getStoreId())))
                .filter(transaction -> to == null || !transaction.getOccurredAt().isAfter(to))
                .filter(transaction -> matchesQuery(transaction, normalizedQuery))
                .filter(transaction -> paymentMethodFilter == null || transaction.getPaymentMethod() == paymentMethodFilter)
                .sorted(resolveSort(sort))
                .map(this::toResponse)
                .toList();

        int fromIndex = Math.min(page * size, filtered.size());
        int toIndex = Math.min(fromIndex + size, filtered.size());
        return new PageImpl<>(filtered.subList(fromIndex, toIndex), PageRequest.of(page, size), filtered.size());
    }

    @Transactional(readOnly = true)
    public SaleTransactionSummaryResponse summary(Instant from, Instant to, String paymentMethod) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(BAD_REQUEST, "from cannot be after to");
        }

        UUID storeId = StoreContext.currentStoreId();
        SalePaymentMethod paymentMethodFilter = parsePaymentMethod(paymentMethod);

        List<SaleTransaction> allTransactions = from == null
                ? saleTransactionRepository.findByDeletedFalseOrderByOccurredAtDesc()
                : saleTransactionRepository.findByOccurredAtGreaterThanEqualAndDeletedFalseOrderByOccurredAtDesc(from);

        List<SaleTransaction> filtered = allTransactions.stream()
                .filter(transaction -> transaction.getStoreId() == null || storeId.equals(transaction.getStoreId()))
                .filter(transaction -> to == null || !transaction.getOccurredAt().isAfter(to))
                .filter(transaction -> paymentMethodFilter == null || transaction.getPaymentMethod() == paymentMethodFilter)
                .toList();

        BigDecimal grossAmount = filtered.stream()
                .map(SaleTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal refundedAmount = filtered.stream()
                .map(transaction -> transaction.getRefundedAmount() == null ? BigDecimal.ZERO : transaction.getRefundedAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<SalePaymentMethodSummaryResponse> breakdown = filtered.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        transaction -> transaction.getPaymentMethod() == null ? SalePaymentMethod.CASH : transaction.getPaymentMethod()
                ))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    BigDecimal gross = entry.getValue().stream()
                            .map(SaleTransaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal refunded = entry.getValue().stream()
                            .map(transaction -> transaction.getRefundedAmount() == null ? BigDecimal.ZERO : transaction.getRefundedAmount())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new SalePaymentMethodSummaryResponse(
                            entry.getKey().name(),
                            entry.getValue().size(),
                            gross,
                            refunded,
                            gross.subtract(refunded)
                    );
                })
                .toList();

        return new SaleTransactionSummaryResponse(
                filtered.size(),
                grossAmount,
                refundedAmount,
                grossAmount.subtract(refundedAmount),
                breakdown
        );
    }

    @Transactional(readOnly = true)
    public SaleTransactionDetailResponse detail(UUID transactionId) {
        SaleTransaction transaction = saleTransactionRepository.findByIdAndDeletedFalse(transactionId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Sale transaction not found"));

        UUID storeId = StoreContext.currentStoreId();
        if (transaction.getStoreId() != null && !storeId.equals(transaction.getStoreId())) {
            throw new ResponseStatusException(NOT_FOUND, "Sale transaction not found");
        }

        List<SaleTransactionTimelineEventResponse> timeline = activityLogRepository
                .findByStoreIdAndResourceTypeAndResourceIdAndDeletedFalseOrderByOccurredAtDesc(
                        storeId,
                        "SALE_TRANSACTION",
                        String.valueOf(transactionId)
                )
                .stream()
                .map(this::toTimelineResponse)
                .toList();

        return toDetailResponse(transaction, transactionId, timeline);
    }

    @Transactional
    public SaleTransactionResponse refund(UUID transactionId, RefundSaleTransactionRequest request) {
        if (request == null || request.amount() == null || request.amount().signum() <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "refund amount must be greater than zero");
        }

        SaleTransaction transaction = saleTransactionRepository.findByIdAndDeletedFalse(transactionId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Sale transaction not found"));

        if (transaction.getStatus() == SaleTransactionStatus.CANCELED) {
            throw new ResponseStatusException(BAD_REQUEST, "Canceled transaction cannot be refunded");
        }

        BigDecimal currentRefunded = transaction.getRefundedAmount() == null ? BigDecimal.ZERO : transaction.getRefundedAmount();
        BigDecimal nextRefunded = currentRefunded.add(request.amount());
        if (nextRefunded.compareTo(transaction.getAmount()) > 0) {
            throw new ResponseStatusException(BAD_REQUEST, "Refund amount exceeds transaction amount");
        }

        if (transaction.getInventoryItemId() != null && transaction.getInventoryQuantity() != null && !transaction.isStockReverted()) {
            inventoryStockCoordinator.release(
                    transaction.getInventoryItemId(),
                    transaction.getInventoryQuantity(),
                    "SALE refund rollback " + transaction.getId(),
                    "SALE_TRANSACTION_REFUND",
                    transaction.getId(),
                    "sale:" + transaction.getId() + ":refund-rollback"
            );
            transaction.setStockReverted(true);
        }

        transaction.setRefundedAmount(nextRefunded);
        transaction.setRefundedAt(Instant.now());
        if (nextRefunded.compareTo(transaction.getAmount()) == 0) {
            transaction.setStatus(SaleTransactionStatus.REFUNDED);
        }

        securityAuditService.log(
                AuditEventType.SALE_TRANSACTION_REFUNDED,
                null,
                "SALE_TRANSACTION",
                String.valueOf(transaction.getId()),
                "{\"refundAmount\":" + request.amount() + "}"
        );

        return toResponse(transaction);
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
                transaction.getPaymentMethod() == null ? SalePaymentMethod.CASH.name() : transaction.getPaymentMethod().name(),
                transaction.getPaymentReference(),
                transaction.getCustomerId(),
                transaction.getCustomerName(),
                transaction.getAmount(),
                transaction.getRefundedAmount(),
                transaction.getRefundedAt(),
                transaction.getNotes(),
                transaction.getOccurredAt()
        );
    }

    private SaleTransactionDetailResponse toDetailResponse(SaleTransaction transaction,
                                                           UUID fallbackId,
                                                           List<SaleTransactionTimelineEventResponse> timeline) {
        return new SaleTransactionDetailResponse(
                transaction.getId() == null ? fallbackId : transaction.getId(),
                transaction.getTransactionType().getId(),
                transaction.getTransactionType().getCode(),
                transaction.getReceiptNumber(),
                transaction.getStatus() == null ? SaleTransactionStatus.COMPLETED.name() : transaction.getStatus().name(),
                transaction.getPaymentMethod() == null ? SalePaymentMethod.CASH.name() : transaction.getPaymentMethod().name(),
                transaction.getPaymentReference(),
                transaction.getCustomerId(),
                transaction.getCustomerName(),
                transaction.getAmount(),
                transaction.getRefundedAmount(),
                transaction.getRefundedAt(),
                transaction.getNotes(),
                transaction.getOccurredAt(),
                timeline
        );
    }

    private SaleTransactionTimelineEventResponse toTimelineResponse(ActivityLog log) {
        return new SaleTransactionTimelineEventResponse(
                log.getId(),
                log.getAction(),
                log.getResourceType(),
                log.getResourceId(),
                log.getAfterJson(),
                log.getOccurredAt()
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

    private Comparator<SaleTransaction> resolveSort(String sort) {
        String normalized = trimToNull(sort);
        String field = "occurredAt";
        String direction = "desc";

        if (normalized != null) {
            String[] parts = normalized.split(",");
            field = parts[0].trim();
            if (parts.length > 1) {
                direction = parts[1].trim().toLowerCase(Locale.ROOT);
            }
        }

        Comparator<SaleTransaction> comparator = switch (field) {
            case "amount" -> Comparator.comparing(SaleTransaction::getAmount, Comparator.nullsLast(BigDecimal::compareTo));
            case "receiptNumber" -> Comparator.comparing(SaleTransaction::getReceiptNumber, Comparator.nullsLast(String::compareTo));
            case "customerName" -> Comparator.comparing(SaleTransaction::getCustomerName, Comparator.nullsLast(String::compareTo));
            case "occurredAt" -> Comparator.comparing(SaleTransaction::getOccurredAt, Comparator.nullsLast(Instant::compareTo));
            default -> throw new ResponseStatusException(BAD_REQUEST, "Unsupported sort field");
        };

        return "asc".equals(direction) ? comparator : comparator.reversed();
    }

    private SalePaymentMethod parsePaymentMethodOrDefault(String value) {
        SalePaymentMethod parsed = parsePaymentMethod(value);
        return parsed == null ? SalePaymentMethod.CASH : parsed;
    }

    private SalePaymentMethod parsePaymentMethod(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            return SalePaymentMethod.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(BAD_REQUEST, "Unsupported payment method");
        }
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
