package com.optimaxx.management.security;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class StoreContext {

    private static final UUID DEFAULT_STORE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private StoreContext() {
    }

    public static UUID currentStoreId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return DEFAULT_STORE_ID;
        }

        Object details = authentication.getDetails();
        if (details instanceof String detailsString && !detailsString.isBlank()) {
            try {
                return UUID.fromString(detailsString);
            } catch (IllegalArgumentException ignored) {
                return DEFAULT_STORE_ID;
            }
        }

        return DEFAULT_STORE_ID;
    }
}
