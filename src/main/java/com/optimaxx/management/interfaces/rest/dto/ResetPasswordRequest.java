package com.optimaxx.management.interfaces.rest.dto;

public record ResetPasswordRequest(String resetToken, String newPassword) {
}
