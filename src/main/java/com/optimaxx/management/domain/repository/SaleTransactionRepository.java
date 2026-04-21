package com.optimaxx.management.domain.repository;

import com.optimaxx.management.domain.model.Customer;
import com.optimaxx.management.domain.model.SaleTransaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleTransactionRepository extends JpaRepository<SaleTransaction, UUID> {

    boolean existsByCustomerAndDeletedFalse(Customer customer);

    long countByCustomerAndDeletedFalse(Customer customer);

    @Query("select coalesce(sum(s.amount), 0) from SaleTransaction s where s.customer = :customer and s.deleted = false")
    BigDecimal sumAmountByCustomer(Customer customer);

    Optional<SaleTransaction> findTopByCustomerAndDeletedFalseOrderByOccurredAtDesc(Customer customer);

    Optional<SaleTransaction> findTopByStoreIdAndReceiptNumberStartingWithOrderByReceiptNumberDesc(UUID storeId, String prefix);

    Optional<SaleTransaction> findTopByStoreIdAndInvoiceNumberStartingWithOrderByInvoiceNumberDesc(UUID storeId, String prefix);

    Optional<SaleTransaction> findByIdAndDeletedFalse(UUID id);

    Optional<SaleTransaction> findByReceiptNumberAndDeletedFalse(String receiptNumber);

    boolean existsByStoreIdAndReceiptNumberAndDeletedFalse(UUID storeId, String receiptNumber);

    List<SaleTransaction> findByDeletedFalseOrderByOccurredAtDesc();

    List<SaleTransaction> findByOccurredAtGreaterThanEqualAndDeletedFalseOrderByOccurredAtDesc(Instant occurredAt);

    // ── Analytics queries ────────────────────────────────────────────────────

    @Query("SELECT coalesce(sum(s.amount), 0) FROM SaleTransaction s " +
           "WHERE s.storeId = :storeId AND s.deleted = false " +
           "  AND s.occurredAt >= :from AND s.occurredAt <= :to " +
           "  AND s.status IN (com.optimaxx.management.domain.model.SaleTransactionStatus.COMPLETED, " +
           "                   com.optimaxx.management.domain.model.SaleTransactionStatus.REFUNDED)")
    BigDecimal sumRevenueInRange(@Param("storeId") UUID storeId,
                                 @Param("from") Instant from,
                                 @Param("to") Instant to);

    @Query("SELECT count(s) FROM SaleTransaction s " +
           "WHERE s.storeId = :storeId AND s.deleted = false " +
           "  AND s.occurredAt >= :from AND s.occurredAt <= :to")
    long countInRange(@Param("storeId") UUID storeId,
                      @Param("from") Instant from,
                      @Param("to") Instant to);

    @Query("SELECT s.transactionType.category, coalesce(sum(s.amount), 0) " +
           "FROM SaleTransaction s " +
           "WHERE s.storeId = :storeId AND s.deleted = false " +
           "  AND s.occurredAt >= :from AND s.occurredAt <= :to " +
           "GROUP BY s.transactionType.category")
    List<Object[]> revenueByCategory(@Param("storeId") UUID storeId,
                                     @Param("from") Instant from,
                                     @Param("to") Instant to);
}
