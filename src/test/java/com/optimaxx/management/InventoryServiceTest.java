package com.optimaxx.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.optimaxx.management.domain.model.InventoryItem;
import com.optimaxx.management.domain.model.InventoryMovement;
import com.optimaxx.management.domain.model.InventoryMovementType;
import com.optimaxx.management.domain.repository.InventoryItemRepository;
import com.optimaxx.management.domain.repository.InventoryMovementRepository;
import com.optimaxx.management.interfaces.rest.dto.AdminCreateInventoryItemRequest;
import com.optimaxx.management.interfaces.rest.dto.InventoryStockChangeRequest;
import com.optimaxx.management.security.InventoryService;
import com.optimaxx.management.security.audit.SecurityAuditService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

class InventoryServiceTest {

    @Test
    void shouldCreateInventoryItem() {
        InventoryItemRepository itemRepository = Mockito.mock(InventoryItemRepository.class);
        InventoryMovementRepository movementRepository = Mockito.mock(InventoryMovementRepository.class);
        SecurityAuditService auditService = Mockito.mock(SecurityAuditService.class);

        when(itemRepository.existsBySkuAndDeletedFalse("SKU-1")).thenReturn(false);
        when(itemRepository.save(any(InventoryItem.class))).thenAnswer(i -> i.getArgument(0));

        InventoryService service = new InventoryService(itemRepository, movementRepository, auditService);
        var response = service.createItem(new AdminCreateInventoryItemRequest("sku-1", "Lens", "LENS", 10, 2));

        assertThat(response.sku()).isEqualTo("SKU-1");
        assertThat(response.quantity()).isEqualTo(10);
    }

    @Test
    void shouldChangeStockOut() {
        InventoryItemRepository itemRepository = Mockito.mock(InventoryItemRepository.class);
        InventoryMovementRepository movementRepository = Mockito.mock(InventoryMovementRepository.class);
        SecurityAuditService auditService = Mockito.mock(SecurityAuditService.class);

        UUID id = UUID.randomUUID();
        InventoryItem item = new InventoryItem();
        item.setSku("SKU-1");
        item.setName("Lens");
        item.setQuantity(10);
        item.setMinQuantity(2);

        when(itemRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(item));
        when(movementRepository.save(any(InventoryMovement.class))).thenAnswer(i -> i.getArgument(0));

        InventoryService service = new InventoryService(itemRepository, movementRepository, auditService);
        var response = service.changeStock(id, new InventoryStockChangeRequest(InventoryMovementType.OUT, 3, "sale"));

        assertThat(response.quantity()).isEqualTo(7);
        verify(movementRepository).save(any(InventoryMovement.class));
    }

    @Test
    void shouldRejectStockOutWhenInsufficient() {
        InventoryItemRepository itemRepository = Mockito.mock(InventoryItemRepository.class);
        InventoryMovementRepository movementRepository = Mockito.mock(InventoryMovementRepository.class);
        SecurityAuditService auditService = Mockito.mock(SecurityAuditService.class);

        UUID id = UUID.randomUUID();
        InventoryItem item = new InventoryItem();
        item.setQuantity(1);

        when(itemRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(item));

        InventoryService service = new InventoryService(itemRepository, movementRepository, auditService);

        assertThatThrownBy(() -> service.changeStock(id, new InventoryStockChangeRequest(InventoryMovementType.OUT, 3, "sale")))
                .isInstanceOf(ResponseStatusException.class);
    }
}
