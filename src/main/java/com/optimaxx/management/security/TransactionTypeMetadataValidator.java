package com.optimaxx.management.security;

import com.optimaxx.management.domain.model.TransactionTypeCategory;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class TransactionTypeMetadataValidator {

    private final ObjectMapper objectMapper;

    private static final Map<TransactionTypeCategory, List<String>> REQUIRED_KEYS = Map.of(
            TransactionTypeCategory.SALE, List.of("receiptTemplate"),
            TransactionTypeCategory.REPAIR, List.of("workflow"),
            TransactionTypeCategory.PRESCRIPTION, List.of("validationRules"),
            TransactionTypeCategory.SERVICE, List.of("serviceLevel")
    );

    public TransactionTypeMetadataValidator() {
        this.objectMapper = new ObjectMapper();
    }

    public void validate(TransactionTypeCategory category, String metadataJson) {
        if (category == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "category is required");
        }
        if (metadataJson == null || metadataJson.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "metadataJson is required for category " + category.name());
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(metadataJson);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "metadataJson must be valid JSON object");
        }

        if (root == null || !root.isObject()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "metadataJson must be valid JSON object");
        }

        List<String> requiredKeys = REQUIRED_KEYS.getOrDefault(category, List.of());
        for (String key : requiredKeys) {
            JsonNode value = root.get(key);
            if (value == null || value.isNull()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "metadataJson missing required key: " + key);
            }
        }
    }
}
