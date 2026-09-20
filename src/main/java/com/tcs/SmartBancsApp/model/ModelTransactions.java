package com.tcs.SmartBancsApp.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Los movimientos confirmados se consultan; no se editan ni se eliminan por la API.
@Entity
@Table(name = "transactions")
@Getter
@NoArgsConstructor
public class ModelTransactions {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID transactionId;

    // Titular que realiza la operacion; en un deposito es el titular del destino.
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "source_account_number", length = 8, updatable = false)
    private String sourceAccountNumber;

    @Column(name = "destination_account_number", length = 8, updatable = false)
    private String destinationAccountNumber;

    @Column(name = "service_code", length = 30, updatable = false)
    private String serviceCode;

    @Column(name = "customer_reference", length = 100, updatable = false)
    private String customerReference;

    @Column(name = "idempotency_key", nullable = false, unique = true, updatable = false)
    private UUID idempotencyKey;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(name = "description", length = 255, updatable = false)
    private String description;

    @Column(name = "type", nullable = false, length = 16, updatable = false)
    private String type;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
