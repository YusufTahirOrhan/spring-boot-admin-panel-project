package com.optimaxx.management.domain.repository;

import com.optimaxx.management.domain.model.Customer;
import com.optimaxx.management.domain.model.RepairOrder;
import com.optimaxx.management.domain.model.RepairStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepairOrderRepository extends JpaRepository<RepairOrder, UUID> {

    boolean existsByCustomerAndDeletedFalse(Customer customer);

    long countByCustomerAndDeletedFalse(Customer customer);

    long countByCustomerAndStatusAndDeletedFalse(Customer customer, RepairStatus status);

    Optional<RepairOrder> findTopByCustomerAndDeletedFalseOrderByReceivedAtDesc(Customer customer);

    Optional<RepairOrder> findByIdAndDeletedFalse(UUID id);

    List<RepairOrder> findByDeletedFalseOrderByReceivedAtDesc();
}
