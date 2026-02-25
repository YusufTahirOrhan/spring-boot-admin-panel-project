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
}
