package com.tcs.SmartBancsApp.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bancs_outbox")
@Getter
@NoArgsConstructor
public class ModelBancsOutbox {
    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "transaction_id", nullable = false, unique = true, updatable = false)
    private UUID transactionId;

    @Column(name = "idempotency_key", nullable = false, unique = true, updatable = false)
    private UUID idempotencyKey;

    @Column(name = "source_account_number", length = 8, updatable = false)
    private String sourceAccountNumber;

    @Column(name = "destination_account_number", length = 8, updatable = false)
    private String destinationAccountNumber;

    @Column(nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 16, updatable = false)
    private String type;

    @Column(length = 255, updatable = false)
    private String description;

    @Column(name = "service_code", length = 30, updatable = false)
    private String serviceCode;

    @Column(name = "customer_reference", length = 100, updatable = false)
    private String customerReference;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "available_at", nullable = false)
    private OffsetDateTime availableAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    @Column(name = "last_error", length = 500)
    private String lastError;

    public ModelBancsOutbox(ModelTransactions transaction) {
        this.eventId = UUID.randomUUID();
        this.transactionId = transaction.getTransactionId();
        this.idempotencyKey = transaction.getIdempotencyKey();
        this.sourceAccountNumber = transaction.getSourceAccountNumber();
        this.destinationAccountNumber = transaction.getDestinationAccountNumber();
        this.amount = transaction.getAmount();
        this.type = transaction.getType();
        this.description = transaction.getDescription();
        this.serviceCode = transaction.getServiceCode();
        this.customerReference = transaction.getCustomerReference();
        this.status = "PENDING";
        this.attempts = 0;
        this.availableAt = OffsetDateTime.now();
        this.createdAt = OffsetDateTime.now();
    }

    public void markProcessing() {
        status = "PROCESSING";
        attempts++;
        lastError = null;
    }

    public void markSent(OffsetDateTime processedAt) {
        status = "SENT";
        this.processedAt = processedAt;
        lastError = null;
    }

    public void markRetry(OffsetDateTime availableAt, String error) {
        status = "PENDING";
        this.availableAt = availableAt;
        lastError = error;
    }

    public void markFailed(OffsetDateTime processedAt, String error) {
        status = "FAILED";
        this.processedAt = processedAt;
        lastError = error;
    }
}
