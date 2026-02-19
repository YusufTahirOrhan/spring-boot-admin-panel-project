package com.optimaxx.management.domain.repository;

import com.optimaxx.management.domain.model.InventoryMovement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, UUID> {

    List<InventoryMovement> findByDeletedFalseOrderByMovedAtDesc();

    Optional<InventoryMovement> findByIdempotencyKeyAndDeletedFalse(String idempotencyKey);
}
