package com.optimaxx.management.interfaces.rest.dto;

import java.util.List;

public record UpdateSitePageRequest(List<SitePageBlockRequest> blocks) {
}
