package com.optimaxx.management.interfaces.rest.dto;

public record AuthLoginResponse(String accessToken, String tokenType, long expiresInMinutes, String role) {
}
