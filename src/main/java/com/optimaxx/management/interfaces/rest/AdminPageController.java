package com.optimaxx.management.interfaces.rest;

import com.optimaxx.management.application.SiteAssetService;
import com.optimaxx.management.application.SitePageService;
import com.optimaxx.management.interfaces.rest.dto.AssetUploadResponse;
import com.optimaxx.management.interfaces.rest.dto.SitePageResponse;
import com.optimaxx.management.interfaces.rest.dto.UpdateSitePageRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/pages")
public class AdminPageController {

    private final SitePageService sitePageService;
    private final SiteAssetService siteAssetService;

    public AdminPageController(SitePageService sitePageService, SiteAssetService siteAssetService) {
        this.sitePageService = sitePageService;
        this.siteAssetService = siteAssetService;
    }

    @GetMapping("/home/draft")
    public SitePageResponse getHomeDraft() {
        return sitePageService.getHomeDraft();
    }

    @PutMapping("/home/draft")
    public SitePageResponse updateHomeDraft(@RequestBody UpdateSitePageRequest request) {
        return sitePageService.updateHomeDraft(request);
    }

    @PostMapping("/home/publish")
    public SitePageResponse publishHomeDraft() {
        return sitePageService.publishHomeDraft();
    }

    @PostMapping(value = "/assets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AssetUploadResponse uploadAsset(@RequestParam("file") MultipartFile file) {
        return siteAssetService.upload(file);
    }
}
