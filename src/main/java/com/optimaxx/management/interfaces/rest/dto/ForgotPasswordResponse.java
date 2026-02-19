package com.optimaxx.management.interfaces.rest.dto;

public record ForgotPasswordResponse(String message, long expiresInMinutes) {
}
