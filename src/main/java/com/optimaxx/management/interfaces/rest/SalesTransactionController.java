package com.optimaxx.management.interfaces.rest;

import com.optimaxx.management.interfaces.rest.dto.CreateSaleTransactionRequest;
import com.optimaxx.management.interfaces.rest.dto.SaleTransactionResponse;
import com.optimaxx.management.interfaces.rest.dto.UpdateSaleTransactionStatusRequest;
import com.optimaxx.management.security.SalesTransactionService;
import java.time.Instant;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.UUID;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sales/transactions")
public class SalesTransactionController {

    private final SalesTransactionService salesTransactionService;

    public SalesTransactionController(SalesTransactionService salesTransactionService) {
        this.salesTransactionService = salesTransactionService;
    }

    @PostMapping
    public SaleTransactionResponse create(@RequestBody CreateSaleTransactionRequest request) {
        return salesTransactionService.create(request);
    }

    @GetMapping
    public List<SaleTransactionResponse> list(@RequestParam(value = "from", required = false)
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                              @RequestParam(value = "q", required = false) String query) {
        return salesTransactionService.list(from, query);
    }

    @PatchMapping("/{transactionId}/status")
    public SaleTransactionResponse updateStatus(@PathVariable UUID transactionId,
                                                @RequestBody UpdateSaleTransactionStatusRequest request) {
        return salesTransactionService.updateStatus(transactionId, request);
    }
}
