package com.optimaxx.management.interfaces.rest;

import com.optimaxx.management.interfaces.rest.dto.CreateRepairOrderRequest;
import com.optimaxx.management.interfaces.rest.dto.RepairOrderResponse;
import com.optimaxx.management.interfaces.rest.dto.UpdateRepairStatusRequest;
import com.optimaxx.management.security.RepairOrderService;
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
@RequestMapping("/api/v1/sales/repairs")
public class SalesRepairController {

    private final RepairOrderService repairOrderService;

    public SalesRepairController(RepairOrderService repairOrderService) {
        this.repairOrderService = repairOrderService;
    }

    @PostMapping
    public RepairOrderResponse create(@RequestBody CreateRepairOrderRequest request) {
        return repairOrderService.create(request);
    }

    @GetMapping
    public List<RepairOrderResponse> list() {
        return repairOrderService.list();
    }

    @PatchMapping("/{repairOrderId}/status")
    public RepairOrderResponse updateStatus(@PathVariable UUID repairOrderId,
                                            @RequestBody UpdateRepairStatusRequest request) {
        return repairOrderService.updateStatus(repairOrderId, request);
    }
}
