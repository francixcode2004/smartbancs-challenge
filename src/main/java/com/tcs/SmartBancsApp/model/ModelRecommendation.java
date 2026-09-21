package com.tcs.SmartBancsApp.model;

import java.time.OffsetDateTime;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "recommendations")
@Getter
@NoArgsConstructor
public class ModelRecommendation {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "account_number", nullable = false, length = 8, updatable = false)
    private String accountNumber;

    @Column(nullable = false, length = 150, updatable = false)
    private String title;

    @Column(nullable = false, length = 600, updatable = false)
    private String message;

    @Column(nullable = false, length = 20, updatable = false)
    private String priority;

    @Column(nullable = false, length = 30, updatable = false)
    private String category;

    @Column(name = "model_name", nullable = false, length = 100, updatable = false)
    private String modelName;

    @Column(name = "prompt_version", nullable = false, length = 50, updatable = false)
    private String promptVersion;

    @Column(nullable = false, length = 20, updatable = false)
    private String source;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public ModelRecommendation(String accountNumber, String title, String message,
            String priority, String category, String modelName, String promptVersion, String source) {
        this.id = UUID.randomUUID();
        this.accountNumber = accountNumber;
        this.title = title;
        this.message = message;
        this.priority = priority;
        this.category = category;
        this.modelName = modelName;
        this.promptVersion = promptVersion;
        this.source = source;
        this.createdAt = OffsetDateTime.now();
    }
}
