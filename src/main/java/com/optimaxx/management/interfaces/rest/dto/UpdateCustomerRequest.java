package com.optimaxx.management.interfaces.rest.dto;

public record UpdateCustomerRequest(String firstName,
                                    String lastName,
                                    String phone,
                                    String email,
                                    String notes) {
}
