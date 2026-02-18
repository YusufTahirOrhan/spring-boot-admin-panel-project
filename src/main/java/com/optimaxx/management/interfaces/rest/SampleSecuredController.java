package com.optimaxx.management.interfaces.rest;

import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class SampleSecuredController {

    @GetMapping("/ping")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public Map<String, String> ping() {
        return Map.of("message", "Admin area is reachable");
    }
}
