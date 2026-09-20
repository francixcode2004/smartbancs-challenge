package com.tcs.SmartBancsApp.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;

// La cuenta de origen se obtiene del token, nunca del cuerpo de la solicitud.
public record TransferRequest(
        @NotBlank @Pattern(regexp = "[0-9]{8}") String destinationAccountNumber,
        @NotNull @DecimalMin("0.01") @Digits(integer = 13, fraction = 2) BigDecimal amount,
        @Size(max = 255) String description) {
}
