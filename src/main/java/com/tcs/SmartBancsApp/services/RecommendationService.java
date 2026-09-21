package com.tcs.SmartBancsApp.services;

import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import com.tcs.SmartBancsApp.dto.RecommendationApiResponse;
import com.tcs.SmartBancsApp.dto.RecommendationMovement;
import com.tcs.SmartBancsApp.model.ModelRecommendation;
import com.tcs.SmartBancsApp.repositories.RepositoryRecommendations;
import com.tcs.SmartBancsApp.repositories.RepositoryTransactions;

@Service
public class RecommendationService {
    private static final Set<String> MOVEMENT_TYPES = Set.of("deposit", "withdraw", "transfer", "service_payment");
    private final RepositoryTransactions transactions;
    private final RepositoryRecommendations recommendations;
    private final RestClient client;

    public RecommendationService(RepositoryTransactions transactions,
            RepositoryRecommendations recommendations,
            @Value("${recommendations.url:http://localhost:8000}") String serviceUrl) {
        this.transactions = transactions;
        this.recommendations = recommendations;
        this.client = RestClient.create(serviceUrl);
    }

    @Async("recommendationExecutor")
    public void refreshAsync(String accountNumber) {
        var movements = transactions.findHistoryByAccount(accountNumber).stream()
            .map(movement -> new RecommendationMovement(
                        movement.getTransactionId().toString(), movement.getSourceAccountNumber(),
                movement.getDestinationAccountNumber(), movement.getAmount(), movement.getType(),
                movement.getServiceCode(), movement.getCreatedAt()))
            .toList();
        refreshAsync(accountNumber, movements);
    }

    @Async("recommendationExecutor")
    public void refreshAsync(String accountNumber, List<RecommendationMovement> movements) {
        try {
            var safeMovements = movements == null ? List.<RecommendationMovement>of() : movements.stream()
                    .filter(movement -> movement != null)
                    .filter(movement -> accountNumber.equals(movement.sourceAccountNumber())
                        || accountNumber.equals(movement.destinationAccountNumber()))
                    .filter(movement -> movement.amount() != null && movement.amount().signum() > 0
                        && movement.createdAt() != null
                        && MOVEMENT_TYPES.contains(movement.type())
                        && validAccountNumber(movement.sourceAccountNumber())
                        && validAccountNumber(movement.destinationAccountNumber()))
                    .toList();
            var response = client.post()
                    .uri("/recommendations")
                    .body(safeMovements)
                    .retrieve()
                    .body(RecommendationApiResponse.class);
            if (response != null && response.recommendations() != null) {
                saveRecommendations(accountNumber, response);
            }
        } catch (RuntimeException ignored) {
            // Una recomendacion nunca puede hacer fallar una transaccion bancaria.
        }
    }

    @Transactional(readOnly = true)
    public ModelRecommendation latest(String accountNumber) {
        return recommendations.findTopByAccountNumberOrderByCreatedAtDesc(accountNumber).orElse(null);
    }

    private boolean validAccountNumber(String accountNumber) {
        return accountNumber == null || accountNumber.matches("[0-9]{8}");
    }

    @Transactional
    protected void saveRecommendations(String accountNumber, RecommendationApiResponse response) {
        for (var item : response.recommendations()) {
            recommendations.save(new ModelRecommendation(accountNumber, item.title(), item.message(),
                    item.priority(), item.category(), response.model(), response.promptVersion(), response.source()));
        }
    }
}
