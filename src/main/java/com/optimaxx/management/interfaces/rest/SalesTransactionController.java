package com.optimaxx.management.interfaces.rest;

import com.optimaxx.management.interfaces.rest.dto.CreateSaleTransactionRequest;
import com.optimaxx.management.interfaces.rest.dto.RefundSaleTransactionRequest;
import com.optimaxx.management.interfaces.rest.dto.SaleTransactionDetailResponse;
import com.optimaxx.management.interfaces.rest.dto.SaleTransactionResponse;
import com.optimaxx.management.interfaces.rest.dto.SaleTransactionSummaryResponse;
import com.optimaxx.management.interfaces.rest.dto.UpdateSaleTransactionStatusRequest;
import com.optimaxx.management.security.SalesTransactionService;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    public Page<SaleTransactionResponse> list(@RequestParam(value = "from", required = false)
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                              @RequestParam(value = "to", required = false)
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                                              @RequestParam(value = "q", required = false) String query,
                                              @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
                                              @RequestParam(value = "page", defaultValue = "0") int page,
                                              @RequestParam(value = "size", defaultValue = "20") int size,
                                              @RequestParam(value = "sort", defaultValue = "occurredAt,desc") String sort) {
        return salesTransactionService.list(from, to, query, paymentMethod, page, size, sort);
    }

    @GetMapping("/summary")
    public SaleTransactionSummaryResponse summary(@RequestParam(value = "from", required = false)
                                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                                  @RequestParam(value = "to", required = false)
                                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                                                  @RequestParam(value = "paymentMethod", required = false) String paymentMethod) {
        return salesTransactionService.summary(from, to, paymentMethod);
    }

    @GetMapping("/export")
    public ResponseEntity<String> exportCsv(@RequestParam(value = "from", required = false)
                                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                            @RequestParam(value = "to", required = false)
                                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                                            @RequestParam(value = "q", required = false) String query,
                                            @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
                                            @RequestParam(value = "sort", defaultValue = "occurredAt,desc") String sort) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sales-transactions.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(salesTransactionService.exportCsv(from, to, query, paymentMethod, sort));
    }

    @GetMapping("/{transactionId}/invoice")
    public ResponseEntity<byte[]> invoice(@PathVariable UUID transactionId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice-" + transactionId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(salesTransactionService.invoicePdf(transactionId));
    }

    @GetMapping("/{transactionId}")
    public SaleTransactionDetailResponse detail(@PathVariable UUID transactionId) {
        return salesTransactionService.detail(transactionId);
    }

    @PatchMapping("/{transactionId}/status")
    public SaleTransactionResponse updateStatus(@PathVariable UUID transactionId,
                                                @RequestBody UpdateSaleTransactionStatusRequest request) {
        return salesTransactionService.updateStatus(transactionId, request);
    }

    @PostMapping("/{transactionId}/refund")
    public SaleTransactionResponse refund(@PathVariable UUID transactionId,
                                          @RequestBody RefundSaleTransactionRequest request) {
        return salesTransactionService.refund(transactionId, request);
    }
}
