package com.optimaxx.management.interfaces.rest.dto;

public record AdminCreateTransactionTypeRequest(String code, String name, Boolean active, Integer sortOrder) {
}
