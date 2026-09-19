package com.tcs.SmartBancsApp.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import com.tcs.SmartBancsApp.dto.TransactionRequest;
import com.tcs.SmartBancsApp.model.ModelTransactions;
import com.tcs.SmartBancsApp.repositories.RepositoryTransactions;
import com.tcs.SmartBancsApp.repositories.RepositoryUsers;

@Service
@Validated
@Transactional(readOnly = true)
public class ServiceTransactions {

    private final RepositoryTransactions repositoryTransactions;
    private final RepositoryUsers repositoryUsers;

    public ServiceTransactions(RepositoryTransactions repositoryTransactions,
            RepositoryUsers repositoryUsers) {
        this.repositoryTransactions = repositoryTransactions;
        this.repositoryUsers = repositoryUsers;
    }

    public List<ModelTransactions> getTransactions(UUID userId) {
        return userId == null
                ? repositoryTransactions.findAllByOrderByCreatedAtDescTransactionIdDesc()
                : repositoryTransactions.findByUserIdOrderByCreatedAtDescTransactionIdDesc(userId);
    }

    public ModelTransactions getTransaction(UUID id) {
        return repositoryTransactions.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Movimiento no encontrado"));
    }

    public ModelTransactions getByIdempotencyKey(UUID key) {
        return repositoryTransactions.findByIdempotencyKey(key).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Token no encontrado"));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class, timeout = 5)
    public CreationResult createTransaction(
            @NotNull UUID key, @NotNull @Valid TransactionRequest request) {
        BigDecimal amount;
        try {
            // Never round a customer's amount silently.
            amount = request.amount().setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El importe no puede contener fracciones de centavo");
        }

        if (!repositoryUsers.existsById(request.userId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }

        // The unique index arbitrates concurrent requests across all application instances.
        // Token and movement are the SAME row and commit or roll back together.
        int inserted = repositoryTransactions.insertOnce(
                UUID.randomUUID(), request.userId(), amount, request.type(), key);

        // READ_COMMITTED gives this SELECT a new snapshot after a competing INSERT commits.
        ModelTransactions saved = repositoryTransactions.findByIdempotencyKey(key)
                .orElseThrow(() -> new IllegalStateException("No se pudo recuperar el movimiento"));

        if (!saved.getUserId().equals(request.userId())
                || saved.getAmount().compareTo(amount) != 0
                || !saved.getType().equals(request.type())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Idempotency-Key ya utilizado con otros datos");
        }

        return new CreationResult(saved, inserted == 1);
    }

    public record CreationResult(ModelTransactions transaction, boolean created) {
    }
}