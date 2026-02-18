package com.optimaxx.management.interfaces.rest.dto;

public record AuthChangePasswordRequest(String currentPassword, String newPassword) {
}
