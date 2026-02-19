package com.optimaxx.management.interfaces.rest.dto;

public record CreateCustomerRequest(String firstName,
                                    String lastName,
                                    String phone,
                                    String email,
                                    String notes) {
}
