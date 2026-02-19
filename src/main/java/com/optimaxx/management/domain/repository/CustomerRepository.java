package com.optimaxx.management.domain.repository;

import com.optimaxx.management.domain.model.Customer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByIdAndDeletedFalse(UUID id);

    List<Customer> findByDeletedFalseOrderByCreatedAtDesc();

    List<Customer> findByDeletedFalseAndFirstNameContainingIgnoreCaseOrDeletedFalseAndLastNameContainingIgnoreCaseOrderByCreatedAtDesc(
            String firstName,
            String lastName
    );
}
