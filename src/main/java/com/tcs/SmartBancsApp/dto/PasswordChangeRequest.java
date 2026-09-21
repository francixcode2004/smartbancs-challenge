package com.tcs.SmartBancsApp.dto;
import jakarta.validation.constraints.*;

public record PasswordChangeRequest(
    @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Size(max = 128) String currentPassword,
        @NotBlank @Size(min = 8, max = 128) String newPassword,
        @NotBlank @Size(min = 8, max = 128) String confirmPassword) {
    @Override public String toString() { return "PasswordChangeRequest[REDACTED]"; }
}
