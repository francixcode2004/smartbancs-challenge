package com.tcs.SmartBancsApp.dto;
import jakarta.validation.constraints.*;

public record PasswordChangeRequest(
        @NotBlank @Pattern(regexp = "[0-9]{8}") String accountNumber,
        @NotBlank @Size(max = 128) String currentPassword,
        @NotBlank @Size(min = 8, max = 128) String newPassword,
        @NotBlank @Size(min = 8, max = 128) String confirmPassword) {
    @Override public String toString() { return "PasswordChangeRequest[REDACTED]"; }
}
