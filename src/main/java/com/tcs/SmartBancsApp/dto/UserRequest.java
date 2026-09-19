package com.tcs.SmartBancsApp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Size(min = 8, max = 128) String password) {
    @Override
    public String toString() {
        return "UserRequest[password=REDACTED]";
    }
}