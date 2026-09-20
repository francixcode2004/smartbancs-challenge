package com.tcs.SmartBancsApp.dto;
import jakarta.validation.constraints.*;

public record ProfileRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 100) String email) {
}
