package com.optimaxx.management.interfaces.rest.dto;

public record ForgotPasswordResponse(String resetToken, long expiresInMinutes) {
}
