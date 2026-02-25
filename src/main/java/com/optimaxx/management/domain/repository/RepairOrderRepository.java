package com.optimaxx.management.domain.repository;

import com.optimaxx.management.domain.model.RepairOrder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepairOrderRepository extends JpaRepository<RepairOrder, UUID> {

    boolean existsByCustomerAndDeletedFalse(com.optimaxx.management.domain.model.Customer customer);

    Optional<RepairOrder> findByIdAndDeletedFalse(UUID id);

    List<RepairOrder> findByDeletedFalseOrderByReceivedAtDesc();
}
