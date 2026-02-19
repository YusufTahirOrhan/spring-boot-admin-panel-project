package com.optimaxx.management.domain.repository;

import com.optimaxx.management.domain.model.InventoryItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    boolean existsBySkuAndDeletedFalse(String sku);

    Optional<InventoryItem> findByIdAndDeletedFalse(UUID id);

    List<InventoryItem> findByDeletedFalseOrderByNameAsc();
}
