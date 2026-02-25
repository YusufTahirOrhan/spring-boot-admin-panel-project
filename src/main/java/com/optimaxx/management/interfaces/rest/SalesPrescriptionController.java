package com.optimaxx.management.interfaces.rest;

import com.optimaxx.management.interfaces.rest.dto.CreateLensPrescriptionRequest;
import com.optimaxx.management.interfaces.rest.dto.LensPrescriptionResponse;
import com.optimaxx.management.interfaces.rest.dto.UpdateLensPrescriptionRequest;
import com.optimaxx.management.security.LensPrescriptionService;
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
@RequestMapping("/api/v1/sales/prescriptions")
public class SalesPrescriptionController {

    private final LensPrescriptionService lensPrescriptionService;

    public SalesPrescriptionController(LensPrescriptionService lensPrescriptionService) {
        this.lensPrescriptionService = lensPrescriptionService;
    }

    @PostMapping
    public LensPrescriptionResponse create(@RequestBody CreateLensPrescriptionRequest request) {
        return lensPrescriptionService.create(request);
    }

    @GetMapping
    public List<LensPrescriptionResponse> list() {
        return lensPrescriptionService.list();
    }

    @GetMapping("/{prescriptionId}")
    public LensPrescriptionResponse get(@PathVariable UUID prescriptionId) {
        return lensPrescriptionService.get(prescriptionId);
    }

    @PatchMapping("/{prescriptionId}")
    public LensPrescriptionResponse update(@PathVariable UUID prescriptionId,
                                           @RequestBody UpdateLensPrescriptionRequest request) {
        return lensPrescriptionService.update(prescriptionId, request);
    }
}
