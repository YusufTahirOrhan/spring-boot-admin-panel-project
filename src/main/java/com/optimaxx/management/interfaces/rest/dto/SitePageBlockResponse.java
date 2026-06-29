package com.optimaxx.management.interfaces.rest.dto;

import java.util.Map;
import java.util.UUID;

public record SitePageBlockResponse(UUID id,
                                    String type,
                                    int order,
                                    boolean enabled,
                                    Map<String, Object> content) {
}
