package com.optimaxx.management.domain.repository;

import com.optimaxx.management.domain.model.LensPrescription;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LensPrescriptionRepository extends JpaRepository<LensPrescription, UUID> {

    boolean existsByCustomerAndDeletedFalse(com.optimaxx.management.domain.model.Customer customer);

    Optional<LensPrescription> findByIdAndDeletedFalse(UUID id);

    List<LensPrescription> findByDeletedFalseOrderByRecordedAtDesc();
}
