package com.optimaxx.management.interfaces.rest.dto;

import java.util.Map;
import java.util.UUID;

public record SitePageBlockRequest(UUID id,
                                   String type,
                                   Integer order,
                                   Boolean enabled,
                                   Map<String, Object> content) {
}
