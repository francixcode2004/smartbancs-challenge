package com.tcs.SmartBancsApp.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record RecommendationMovement(
        String transactionId,
        String sourceAccountNumber,
        String destinationAccountNumber,
        BigDecimal amount,
        String type,
        String serviceCode,
        OffsetDateTime createdAt) {
}
