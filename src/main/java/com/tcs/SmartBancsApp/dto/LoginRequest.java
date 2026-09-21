package com.tcs.SmartBancsApp.dto;

import jakarta.validation.constraints.*;

public record LoginRequest(
    @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Size(max = 128) String password) {
    @Override
    public String toString() {
        return "LoginRequest[password=REDACTED]";
    }
}
