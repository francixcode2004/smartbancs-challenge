package com.tcs.SmartBancsApp.dto;

import java.util.UUID;
import jakarta.validation.constraints.NotNull;

// Una cuenta nueva siempre empieza en cero. El cliente no puede asignarse saldo.
public record AccountRequest(@NotNull UUID userId) {
}
