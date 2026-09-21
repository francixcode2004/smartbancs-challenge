package com.tcs.SmartBancsApp.services;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import com.tcs.SmartBancsApp.dto.RecommendationApiResponse;
import com.tcs.SmartBancsApp.dto.RecommendationMovement;
import com.tcs.SmartBancsApp.model.ModelRecommendation;
import com.tcs.SmartBancsApp.model.ModelTransactions;
import com.tcs.SmartBancsApp.repositories.RepositoryRecommendations;
import com.tcs.SmartBancsApp.repositories.RepositoryTransactions;

@Service
public class RecommendationService {
    private final RepositoryTransactions transactions;
    private final RepositoryRecommendations recommendations;
    private final RestClient client;

    public RecommendationService(RepositoryTransactions transactions,
            RepositoryRecommendations recommendations,
            @Value("${recommendations.url:http://localhost:8000}") String serviceUrl) {
        this.transactions = transactions;
        this.recommendations = recommendations;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(35000);
        this.client = RestClient.builder().baseUrl(serviceUrl).requestFactory(factory).build();
    }

    // El boton espera la respuesta; no se mantiene una transaccion SQL durante la llamada a IA.
    public List<ModelRecommendation> refresh(String accountNumber, List<RecommendationMovement> movements) {
        if (movements == null || movements.size() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Envia hasta 100 movimientos");
        }
        List<UUID> ids;
        try {
            ids = movements.stream().map(m -> UUID.fromString(m.transactionId())).distinct().toList();
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Movimiento invalido");
        }
        var verified = transactions.findAllById(ids);
        if (verified.size() != ids.size() || verified.stream().anyMatch(m ->
                !accountNumber.equals(m.getSourceAccountNumber()) && !accountNumber.equals(m.getDestinationAccountNumber()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes analizar tus propios movimientos");
        }
        // Los importes proceden de PostgreSQL: el navegador no puede inventar datos para el analisis.
        return generate(accountNumber, verified);
    }

    @Async("recommendationExecutor")
    public void refreshAsync(String accountNumber) {
        try {
            generate(accountNumber, transactions.findHistoryByAccount(accountNumber).stream().limit(100).toList());
        } catch (RuntimeException exception) {
            LoggerFactory.getLogger(getClass()).warn("No se pudo generar la recomendacion automatica: {}",
                    exception.getClass().getSimpleName());
        }
    }

    private List<ModelRecommendation> generate(String accountNumber, List<ModelTransactions> movements) {
        // No se envian cuentas, nombres, correos, referencias de clientes ni tokens al proveedor.
        var data = movements.stream().map(m -> Map.of(
                "amount", m.getAmount().toPlainString(), "type", m.getType(),
                "direction", accountNumber.equals(m.getDestinationAccountNumber()) ? "incoming" : "outgoing",
                "serviceCode", m.getServiceCode() == null ? "" : m.getServiceCode(),
                "createdAt", m.getCreatedAt().toString())).toList();
        RecommendationApiResponse response;
        try {
            response = client.post().uri("/recommendations")
                    .body(Map.of("currency", "USD", "movements", data))
                    .retrieve().body(RecommendationApiResponse.class);
        } catch (RestClientException exception) {
            LoggerFactory.getLogger(getClass()).warn("Servicio de recomendaciones no disponible: {}",
                    exception.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No fue posible generar la recomendacion. Revisa el servicio de IA y su configuracion.");
        }
        if (response == null || response.recommendations() == null || response.recommendations().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "La IA no devolvio recomendaciones");
        }
        var result = response.recommendations().stream().limit(3).map(item ->
                new ModelRecommendation(accountNumber, item.title(), item.message(), item.priority(),
                        item.category(), response.model(), response.promptVersion(), response.source())).toList();
        return recommendations.saveAll(result);
    }

    public ModelRecommendation latest(String accountNumber) {
        return recommendations.findTopByAccountNumberOrderByCreatedAtDesc(accountNumber).orElse(null);
    }
}
