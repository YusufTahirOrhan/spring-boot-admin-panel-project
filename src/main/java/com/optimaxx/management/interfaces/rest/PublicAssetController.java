package com.optimaxx.management.interfaces.rest;

import com.optimaxx.management.application.SiteAssetService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/assets")
public class PublicAssetController {

    private final SiteAssetService siteAssetService;

    public PublicAssetController(SiteAssetService siteAssetService) {
        this.siteAssetService = siteAssetService;
    }

    @GetMapping("/site/{filename}")
    public ResponseEntity<Resource> getSiteAsset(@PathVariable String filename) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .contentType(MediaType.parseMediaType(siteAssetService.contentType(filename)))
                .body(siteAssetService.load(filename));
    }
}
