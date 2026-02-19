package com.optimaxx.management.interfaces.rest.dto;

public record AdminUpdateTransactionTypeRequest(String name, Boolean active, Integer sortOrder) {
}
