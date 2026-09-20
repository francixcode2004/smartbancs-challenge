package com.tcs.SmartBancsApp.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public record CashRequest(
        @NotBlank @Pattern(regexp = "[0-9]{8}") String accountNumber,
        @NotNull @DecimalMin("0.01") @Digits(integer = 13, fraction = 2) BigDecimal amount,
        @Size(max = 255) String description) {
}
