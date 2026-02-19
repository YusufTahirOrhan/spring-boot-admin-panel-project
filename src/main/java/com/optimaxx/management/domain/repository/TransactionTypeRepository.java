package com.optimaxx.management.domain.repository;

import com.optimaxx.management.domain.model.TransactionType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionTypeRepository extends JpaRepository<TransactionType, UUID> {

    boolean existsByCodeAndDeletedFalse(String code);

    Optional<TransactionType> findByIdAndDeletedFalse(UUID id);

    List<TransactionType> findByDeletedFalseOrderBySortOrderAscNameAsc();

    List<TransactionType> findByActiveTrueAndDeletedFalseOrderBySortOrderAscNameAsc();
}
