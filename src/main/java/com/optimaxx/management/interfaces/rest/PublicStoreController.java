package com.optimaxx.management.interfaces.rest;

import com.optimaxx.management.application.PublicContactService;
import com.optimaxx.management.interfaces.rest.dto.ContactRequest;
import jakarta.validation.Valid;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/public")
public class PublicStoreController {

    private final PublicContactService publicContactService;

    public PublicStoreController(PublicContactService publicContactService) {
        this.publicContactService = publicContactService;
    }

    @GetMapping("/services")
    @Cacheable(value = "storeInfo", key = "'services'")
    public ResponseEntity<List<Map<String, String>>> getAvailableServices() {
        return ResponseEntity.ok(List.of(
                Map.of("id", "SRV-01", "name", "Frame Repair", "description", "Professional adjustment and repair of optical frames."),
                Map.of("id", "SRV-02", "name", "Eye Exam", "description", "Comprehensive eye examination by our on-site optometrist."),
                Map.of("id", "SRV-03", "name", "Lens Coating", "description", "Applying Blue Light and Anti-Reflective coatings to existing lenses.")
        ));
    }

    @GetMapping("/store-info")
    @Cacheable(value = "storeInfo", key = "'details'")
    public ResponseEntity<Map<String, Object>> getStoreInformation() {
        return ResponseEntity.ok(Map.of(
                "name", "OptiMaxx Central",
                "phone", "+90 555 123 4567",
                "email", "contact@optimaxx.com",
                "address", "123 Vision Avenue, Central District",
                "hours", Map.of(
                        "monday_friday", "09:00 - 19:00",
                        "saturday", "10:00 - 17:00",
                        "sunday", "Closed"
                )
        ));
    }

    @PostMapping("/contact")
    public ResponseEntity<Map<String, String>> submitContactForm(@Valid @RequestBody ContactRequest request) {
        publicContactService.submitContactForm(request);
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Your message has been received. Our team will contact you shortly."
        ));
    }
}
