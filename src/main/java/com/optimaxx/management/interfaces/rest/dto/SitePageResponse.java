package com.optimaxx.management.interfaces.rest.dto;

import java.util.List;

public record SitePageResponse(String page,
                               boolean published,
                               List<SitePageBlockResponse> blocks) {
}
