package com.tcs.SmartBancsApp.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record UserResponse(String accountNumber, String name, String email,
        BigDecimal balance, OffsetDateTime createdAt) {
}
