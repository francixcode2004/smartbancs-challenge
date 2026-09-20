package com.tcs.SmartBancsApp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "basic_services")
@Getter
@NoArgsConstructor
public class ModelBasicServices {
    @Id
    @Column(name = "code", length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "account_number", nullable = false, unique = true, length = 8)
    private String accountNumber;
}
