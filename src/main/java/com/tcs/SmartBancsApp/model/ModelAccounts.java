package com.tcs.SmartBancsApp.model;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
public class ModelAccounts {
    @Id
    @Column(name = "account_number", length = 8, updatable = false)
    private String accountNumber;

    // NULL identifica una cuenta recaudadora del catalogo de servicios.
    @Column(name = "user_id", updatable = false)
    private UUID userId;

    @Column(name = "balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = new BigDecimal("0.00");
}
