package com.optimaxx.management.interfaces.rest;

import com.optimaxx.management.interfaces.rest.dto.AdminCreateTransactionTypeRequest;
import com.optimaxx.management.interfaces.rest.dto.AdminUpdateTransactionTypeRequest;
import com.optimaxx.management.interfaces.rest.dto.TransactionTypeResponse;
import com.optimaxx.management.security.TransactionTypeManagementService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/transaction-types")
public class AdminTransactionTypeController {

    private final TransactionTypeManagementService transactionTypeManagementService;

    public AdminTransactionTypeController(TransactionTypeManagementService transactionTypeManagementService) {
        this.transactionTypeManagementService = transactionTypeManagementService;
    }

    @PostMapping
    public TransactionTypeResponse create(@RequestBody AdminCreateTransactionTypeRequest request) {
        return transactionTypeManagementService.create(request);
    }

    @GetMapping
    public List<TransactionTypeResponse> list() {
        return transactionTypeManagementService.listAdmin();
    }

    @PatchMapping("/{transactionTypeId}")
    public TransactionTypeResponse update(@PathVariable UUID transactionTypeId,
                                          @RequestBody AdminUpdateTransactionTypeRequest request) {
        return transactionTypeManagementService.update(transactionTypeId, request);
    }

    @DeleteMapping("/{transactionTypeId}")
    public ResponseEntity<Void> delete(@PathVariable UUID transactionTypeId) {
        transactionTypeManagementService.softDelete(transactionTypeId);
        return ResponseEntity.noContent().build();
    }
}
