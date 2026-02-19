package com.optimaxx.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.optimaxx.management.domain.model.TransactionType;
import com.optimaxx.management.domain.repository.TransactionTypeRepository;
import com.optimaxx.management.interfaces.rest.dto.AdminCreateTransactionTypeRequest;
import com.optimaxx.management.interfaces.rest.dto.AdminUpdateTransactionTypeRequest;
import com.optimaxx.management.security.TransactionTypeManagementService;
import com.optimaxx.management.security.audit.SecurityAuditService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

class TransactionTypeManagementServiceTest {

    @Test
    void shouldCreateTransactionType() {
        TransactionTypeRepository repository = Mockito.mock(TransactionTypeRepository.class);
        SecurityAuditService securityAuditService = Mockito.mock(SecurityAuditService.class);

        when(repository.existsByCodeAndDeletedFalse("GLASS_SALE")).thenReturn(false);
        when(repository.save(any(TransactionType.class))).thenAnswer(invocation -> {
            TransactionType item = invocation.getArgument(0);
            item.setCreatedAt(java.time.Instant.now());
            return item;
        });

        TransactionTypeManagementService service = new TransactionTypeManagementService(repository, securityAuditService);

        var response = service.create(new AdminCreateTransactionTypeRequest("glass_sale", "Glass Sale", true, 1, com.optimaxx.management.domain.model.TransactionTypeCategory.SALE, "{\"channel\":\"retail\"}"));

        assertThat(response.code()).isEqualTo("GLASS_SALE");
        assertThat(response.name()).isEqualTo("Glass Sale");
        verify(repository).save(any(TransactionType.class));
    }

    @Test
    void shouldRejectDuplicateCode() {
        TransactionTypeRepository repository = Mockito.mock(TransactionTypeRepository.class);
        SecurityAuditService securityAuditService = Mockito.mock(SecurityAuditService.class);

        when(repository.existsByCodeAndDeletedFalse("GLASS_SALE")).thenReturn(true);

        TransactionTypeManagementService service = new TransactionTypeManagementService(repository, securityAuditService);

        assertThatThrownBy(() -> service.create(new AdminCreateTransactionTypeRequest("glass_sale", "Glass Sale", true, 1, com.optimaxx.management.domain.model.TransactionTypeCategory.SALE, "{\"channel\":\"retail\"}")))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldListOnlyActiveForSales() {
        TransactionTypeRepository repository = Mockito.mock(TransactionTypeRepository.class);
        SecurityAuditService securityAuditService = Mockito.mock(SecurityAuditService.class);

        TransactionType active = new TransactionType();
        active.setCode("LENS_ORDER");
        active.setName("Lens Order");
        active.setActive(true);
        active.setSortOrder(2);

        when(repository.findByActiveTrueAndDeletedFalseOrderBySortOrderAscNameAsc()).thenReturn(List.of(active));

        TransactionTypeManagementService service = new TransactionTypeManagementService(repository, securityAuditService);

        assertThat(service.listSales()).hasSize(1);
    }

    @Test
    void shouldUpdateTransactionType() {
        TransactionTypeRepository repository = Mockito.mock(TransactionTypeRepository.class);
        SecurityAuditService securityAuditService = Mockito.mock(SecurityAuditService.class);

        UUID id = UUID.randomUUID();
        TransactionType transactionType = new TransactionType();
        transactionType.setCode("FRAME_REPAIR");
        transactionType.setName("Frame Repair");
        transactionType.setActive(true);
        transactionType.setSortOrder(3);

        when(repository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(transactionType));

        TransactionTypeManagementService service = new TransactionTypeManagementService(repository, securityAuditService);

        var response = service.update(id, new AdminUpdateTransactionTypeRequest("Repair Service", false, 5, com.optimaxx.management.domain.model.TransactionTypeCategory.REPAIR, "{\"requiresSerial\":true}"));

        assertThat(response.name()).isEqualTo("Repair Service");
        assertThat(response.active()).isFalse();
        assertThat(response.sortOrder()).isEqualTo(5);
    }
}
