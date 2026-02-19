package com.optimaxx.management.security;

import com.optimaxx.management.domain.model.InventoryItem;
import com.optimaxx.management.domain.model.InventoryMovement;
import com.optimaxx.management.domain.model.InventoryMovementType;
import com.optimaxx.management.domain.repository.InventoryItemRepository;
import com.optimaxx.management.domain.repository.InventoryMovementRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Component
public class InventoryStockCoordinator {

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryMovementRepository inventoryMovementRepository;

    public InventoryStockCoordinator(InventoryItemRepository inventoryItemRepository,
                                     InventoryMovementRepository inventoryMovementRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
    }

    public InventoryItem consume(UUID inventoryItemId,
                                 int quantity,
                                 String reason,
                                 String sourceType,
                                 UUID sourceId,
                                 String idempotencyKey) {
        if (idempotencyKey != null) {
            var existing = inventoryMovementRepository.findByIdempotencyKeyAndDeletedFalse(idempotencyKey);
            if (existing.isPresent()) {
                return existing.get().getInventoryItem();
            }
        }

        InventoryItem item = inventoryItemRepository.findByIdAndDeletedFalse(inventoryItemId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Inventory item not found"));

        if (quantity <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "inventoryQuantity must be greater than zero");
        }
        if (item.getQuantity() < quantity) {
            throw new ResponseStatusException(BAD_REQUEST, "Insufficient stock");
        }

        item.setQuantity(item.getQuantity() - quantity);

        InventoryMovement movement = new InventoryMovement();
        movement.setInventoryItem(item);
        movement.setMovementType(InventoryMovementType.OUT);
        movement.setQuantityDelta(-quantity);
        movement.setReason(reason);
        movement.setMovedAt(Instant.now());
        movement.setSourceType(sourceType);
        movement.setSourceId(sourceId);
        movement.setIdempotencyKey(idempotencyKey);
        movement.setStoreId(UUID.randomUUID());
        movement.setDeleted(false);
        inventoryMovementRepository.save(movement);

        return item;
    }

    public InventoryItem release(UUID inventoryItemId,
                                 int quantity,
                                 String reason,
                                 String sourceType,
                                 UUID sourceId,
                                 String idempotencyKey) {
        if (idempotencyKey != null) {
            var existing = inventoryMovementRepository.findByIdempotencyKeyAndDeletedFalse(idempotencyKey);
            if (existing.isPresent()) {
                return existing.get().getInventoryItem();
            }
        }

        InventoryItem item = inventoryItemRepository.findByIdAndDeletedFalse(inventoryItemId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Inventory item not found"));

        if (quantity <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "inventoryQuantity must be greater than zero");
        }

        item.setQuantity(item.getQuantity() + quantity);

        InventoryMovement movement = new InventoryMovement();
        movement.setInventoryItem(item);
        movement.setMovementType(InventoryMovementType.IN);
        movement.setQuantityDelta(quantity);
        movement.setReason(reason);
        movement.setMovedAt(Instant.now());
        movement.setSourceType(sourceType);
        movement.setSourceId(sourceId);
        movement.setIdempotencyKey(idempotencyKey);
        movement.setStoreId(UUID.randomUUID());
        movement.setDeleted(false);
        inventoryMovementRepository.save(movement);

        return item;
    }
}
