package com.optimaxx.management.interfaces.rest;

import com.optimaxx.management.interfaces.rest.dto.AdminCreateInventoryItemRequest;
import com.optimaxx.management.interfaces.rest.dto.AdminUpdateInventoryItemRequest;
import com.optimaxx.management.interfaces.rest.dto.InventoryItemResponse;
import com.optimaxx.management.interfaces.rest.dto.InventoryStockChangeRequest;
import com.optimaxx.management.security.InventoryService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/inventory/items")
public class AdminInventoryController {

    private final InventoryService inventoryService;

    public AdminInventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    public InventoryItemResponse create(@RequestBody AdminCreateInventoryItemRequest request) {
        return inventoryService.createItem(request);
    }

    @GetMapping
    public List<InventoryItemResponse> list() {
        return inventoryService.listItems();
    }

    @PatchMapping("/{itemId}")
    public InventoryItemResponse update(@PathVariable UUID itemId,
                                        @RequestBody AdminUpdateInventoryItemRequest request) {
        return inventoryService.updateItem(itemId, request);
    }

    @PostMapping("/{itemId}/stock")
    public InventoryItemResponse changeStock(@PathVariable UUID itemId,
                                             @RequestBody InventoryStockChangeRequest request) {
        return inventoryService.changeStock(itemId, request);
    }
}
