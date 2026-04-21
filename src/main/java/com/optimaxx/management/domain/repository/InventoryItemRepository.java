package com.optimaxx.management.domain.repository;

import com.optimaxx.management.domain.model.InventoryItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    boolean existsBySkuAndDeletedFalse(String sku);

    Optional<InventoryItem> findByIdAndDeletedFalse(UUID id);

    List<InventoryItem> findByDeletedFalseOrderByNameAsc();

    org.springframework.data.domain.Page<InventoryItem> findByCategoryAndQuantityGreaterThanAndDeletedFalse(String category, int quantity, org.springframework.data.domain.Pageable pageable);

    // ── Analytics queries ────────────────────────────────────────────────────

    @Query("SELECT i FROM InventoryItem i WHERE i.deleted = false AND i.quantity <= i.minQuantity ORDER BY (i.quantity - i.minQuantity) ASC")
    List<InventoryItem> findLowStockItems();
}
