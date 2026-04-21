package com.optimaxx.management.interfaces.rest;

import com.optimaxx.management.application.PublicCatalogService;
import com.optimaxx.management.domain.model.InventoryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/public/catalog")
public class PublicCatalogController {

    private final PublicCatalogService publicCatalogService;

    public PublicCatalogController(PublicCatalogService publicCatalogService) {
        this.publicCatalogService = publicCatalogService;
    }

    @GetMapping("/frames")
    public ResponseEntity<Page<InventoryItem>> getAvailableFrames(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<InventoryItem> frames = publicCatalogService.getAvailableFrames(pageable);
        return ResponseEntity.ok(frames);
    }

    @GetMapping("/lenses")
    public ResponseEntity<Page<InventoryItem>> getAvailableLenses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<InventoryItem> lenses = publicCatalogService.getAvailableLenses(pageable);
        return ResponseEntity.ok(lenses);
    }
}
