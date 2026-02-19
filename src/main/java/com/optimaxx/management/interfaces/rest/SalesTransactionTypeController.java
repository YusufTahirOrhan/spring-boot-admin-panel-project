package com.optimaxx.management.interfaces.rest;

import com.optimaxx.management.interfaces.rest.dto.TransactionTypeResponse;
import com.optimaxx.management.security.TransactionTypeManagementService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sales/transaction-types")
public class SalesTransactionTypeController {

    private final TransactionTypeManagementService transactionTypeManagementService;

    public SalesTransactionTypeController(TransactionTypeManagementService transactionTypeManagementService) {
        this.transactionTypeManagementService = transactionTypeManagementService;
    }

    @GetMapping
    public List<TransactionTypeResponse> list() {
        return transactionTypeManagementService.listSales();
    }
}
