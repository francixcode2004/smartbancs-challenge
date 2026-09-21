package com.tcs.SmartBancsApp.integration.bancs;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface BancsClient {
    void sendBatch(List<BancsTransactionEvent> events);

    record BancsTransactionEvent(
            UUID eventId,
            UUID transactionId,
            UUID idempotencyKey,
            String sourceAccountNumber,
            String destinationAccountNumber,
            BigDecimal amount,
            String type,
            String description,
            String serviceCode,
            String customerReference) {
    }
}
