package com.optimaxx.management.interfaces.rest;

import com.optimaxx.management.interfaces.rest.dto.CreateCustomerRequest;
import com.optimaxx.management.interfaces.rest.dto.CustomerResponse;
import com.optimaxx.management.interfaces.rest.dto.UpdateCustomerRequest;
import com.optimaxx.management.security.CustomerService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sales/customers")
public class SalesCustomerController {

    private final CustomerService customerService;

    public SalesCustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public CustomerResponse create(@RequestBody CreateCustomerRequest request) {
        return customerService.create(request);
    }

    @GetMapping
    public List<CustomerResponse> list(@RequestParam(value = "q", required = false) String query) {
        return customerService.list(query);
    }

    @GetMapping("/{customerId}")
    public CustomerResponse get(@PathVariable UUID customerId) {
        return customerService.get(customerId);
    }

    @PatchMapping("/{customerId}")
    public CustomerResponse update(@PathVariable UUID customerId,
                                   @RequestBody UpdateCustomerRequest request) {
        return customerService.update(customerId, request);
    }
}
