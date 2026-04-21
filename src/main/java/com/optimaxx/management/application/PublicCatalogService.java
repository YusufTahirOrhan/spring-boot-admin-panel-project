package com.optimaxx.management.application;

import com.optimaxx.management.domain.model.InventoryItem;
import com.optimaxx.management.domain.repository.InventoryItemRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class PublicCatalogService {

    private final InventoryItemRepository inventoryItemRepository;

    public PublicCatalogService(InventoryItemRepository inventoryItemRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
    }

    @Cacheable(value = "publicCatalog", key = "'frames_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<InventoryItem> getAvailableFrames(Pageable pageable) {
        return inventoryItemRepository.findByCategoryAndQuantityGreaterThanAndDeletedFalse("Frames", 0, pageable);
    }

    @Cacheable(value = "publicCatalog", key = "'lenses_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<InventoryItem> getAvailableLenses(Pageable pageable) {
        return inventoryItemRepository.findByCategoryAndQuantityGreaterThanAndDeletedFalse("Lenses", 0, pageable);
    }
}
