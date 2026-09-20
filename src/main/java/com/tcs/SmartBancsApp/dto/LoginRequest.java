package com.tcs.SmartBancsApp.dto;

import jakarta.validation.constraints.*;

public record LoginRequest(
        @NotBlank @Pattern(regexp = "[0-9]{8}") String accountNumber,
        @NotBlank @Size(max = 128) String password) {
    @Override
    public String toString() {
        return "LoginRequest[password=REDACTED]";
    }
}
