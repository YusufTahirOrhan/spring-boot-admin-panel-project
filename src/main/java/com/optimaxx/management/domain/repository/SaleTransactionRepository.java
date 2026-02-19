package com.optimaxx.management.domain.repository;

import com.optimaxx.management.domain.model.SaleTransaction;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleTransactionRepository extends JpaRepository<SaleTransaction, UUID> {

    List<SaleTransaction> findByDeletedFalseOrderByOccurredAtDesc();

    List<SaleTransaction> findByOccurredAtGreaterThanEqualAndDeletedFalseOrderByOccurredAtDesc(Instant occurredAt);
}
