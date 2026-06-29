package com.optimaxx.management.interfaces.rest;

import com.optimaxx.management.application.SitePageService;
import com.optimaxx.management.interfaces.rest.dto.SitePageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/pages")
public class PublicPageController {

    private final SitePageService sitePageService;

    public PublicPageController(SitePageService sitePageService) {
        this.sitePageService = sitePageService;
    }

    @GetMapping("/home")
    public SitePageResponse getHomePage() {
        return sitePageService.getPublishedHomePage();
    }
}
