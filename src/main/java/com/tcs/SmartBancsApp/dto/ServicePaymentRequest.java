package com.tcs.SmartBancsApp.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public record ServicePaymentRequest(
        @NotBlank @Pattern(regexp = "[A-Z][A-Z0-9_]{1,29}") String serviceCode,
        @NotBlank @Size(max = 100) String customerReference,
        @NotNull @DecimalMin("0.01") @Digits(integer = 13, fraction = 2) BigDecimal amount,
        @Size(max = 255) String description) {
}
