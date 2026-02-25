package com.optimaxx.management.security;

import com.optimaxx.management.domain.model.InventoryItem;
import com.optimaxx.management.domain.model.InventoryMovement;
import com.optimaxx.management.domain.model.InventoryMovementType;
import com.optimaxx.management.domain.repository.InventoryItemRepository;
import com.optimaxx.management.domain.repository.InventoryMovementRepository;
import com.optimaxx.management.interfaces.rest.dto.AdminCreateInventoryItemRequest;
import com.optimaxx.management.interfaces.rest.dto.AdminUpdateInventoryItemRequest;
import com.optimaxx.management.interfaces.rest.dto.InventoryItemResponse;
import com.optimaxx.management.interfaces.rest.dto.InventoryStockChangeRequest;
import com.optimaxx.management.security.audit.AuditEventType;
import com.optimaxx.management.security.audit.SecurityAuditService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class InventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final SecurityAuditService securityAuditService;

    public InventoryService(InventoryItemRepository inventoryItemRepository,
                            InventoryMovementRepository inventoryMovementRepository,
                            SecurityAuditService securityAuditService) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.securityAuditService = securityAuditService;
    }

    @Transactional
    public InventoryItemResponse createItem(AdminCreateInventoryItemRequest request) {
        if (request == null || isBlank(request.sku()) || isBlank(request.name())) {
            throw new ResponseStatusException(BAD_REQUEST, "sku and name are required");
        }

        String normalizedSku = request.sku().trim().toUpperCase();
        if (inventoryItemRepository.existsBySkuAndDeletedFalse(normalizedSku)) {
            throw new ResponseStatusException(BAD_REQUEST, "SKU already exists");
        }

        InventoryItem item = new InventoryItem();
        item.setSku(normalizedSku);
        item.setName(request.name().trim());
        item.setCategory(trimToNull(request.category()));
        item.setQuantity(request.quantity() == null ? 0 : request.quantity());
        item.setMinQuantity(request.minQuantity() == null ? 0 : request.minQuantity());
        item.setStoreId(StoreContext.currentStoreId());
        item.setDeleted(false);

        InventoryItem saved = inventoryItemRepository.save(item);

        securityAuditService.log(AuditEventType.INVENTORY_ITEM_CREATED, null, "INVENTORY", saved.getSku(), "{\"quantity\":" + saved.getQuantity() + "}");
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<InventoryItemResponse> listItems() {
        var storeId = StoreContext.currentStoreId();
        return inventoryItemRepository.findByDeletedFalseOrderByNameAsc().stream()
                .filter(item -> (item.getStoreId() == null || storeId.equals(item.getStoreId())))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public InventoryItemResponse updateItem(UUID itemId, AdminUpdateInventoryItemRequest request) {
        if (request == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Update payload is required");
        }

        InventoryItem item = inventoryItemRepository.findByIdAndDeletedFalse(itemId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Inventory item not found"));

        if (!isBlank(request.name())) {
            item.setName(request.name().trim());
        }
        if (request.category() != null) {
            item.setCategory(trimToNull(request.category()));
        }
        if (request.minQuantity() != null) {
            item.setMinQuantity(request.minQuantity());
        }

        return toResponse(item);
    }

    @Transactional
    public InventoryItemResponse changeStock(UUID itemId, InventoryStockChangeRequest request) {
        if (request == null || request.movementType() == null || request.quantity() == null || request.quantity() <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "movementType and positive quantity are required");
        }

        InventoryItem item = inventoryItemRepository.findByIdAndDeletedFalse(itemId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Inventory item not found"));

        int delta = switch (request.movementType()) {
            case IN -> request.quantity();
            case OUT -> -request.quantity();
            case ADJUST -> request.quantity();
        };

        if (request.movementType() == InventoryMovementType.OUT && item.getQuantity() < request.quantity()) {
            throw new ResponseStatusException(BAD_REQUEST, "Insufficient stock");
        }

        if (request.movementType() == InventoryMovementType.ADJUST) {
            item.setQuantity(request.quantity());
        } else {
            item.setQuantity(item.getQuantity() + delta);
        }

        InventoryMovement movement = new InventoryMovement();
        movement.setInventoryItem(item);
        movement.setMovementType(request.movementType());
        movement.setQuantityDelta(delta);
        movement.setReason(trimToNull(request.reason()));
        movement.setMovedAt(Instant.now());
        movement.setStoreId(StoreContext.currentStoreId());
        movement.setDeleted(false);
        inventoryMovementRepository.save(movement);

        securityAuditService.log(AuditEventType.INVENTORY_STOCK_CHANGED, null, "INVENTORY", item.getSku(), "{\"movement\":\"" + request.movementType().name() + "\",\"delta\":" + delta + "}");

        return toResponse(item);
    }

    private InventoryItemResponse toResponse(InventoryItem item) {
        return new InventoryItemResponse(item.getId(), item.getSku(), item.getName(), item.getCategory(), item.getQuantity(), item.getMinQuantity());
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
