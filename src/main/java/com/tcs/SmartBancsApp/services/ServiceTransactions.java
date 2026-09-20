package com.tcs.SmartBancsApp.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;
import com.tcs.SmartBancsApp.dto.*;
import com.tcs.SmartBancsApp.model.*;
import com.tcs.SmartBancsApp.repositories.*;

@Service
@Validated
@Transactional(readOnly = true)
public class ServiceTransactions {
    private static final BigDecimal MAX_BALANCE = new BigDecimal("9999999999999.99");
    private final RepositoryTransactions transactions;
    private final RepositoryAccounts accounts;
    private final RepositoryBasicServices basicServices;
    private final ServiceAccounts serviceAccounts;
    private final CurrentUser currentUser;

    public ServiceTransactions(RepositoryTransactions transactions, RepositoryAccounts accounts,
            RepositoryBasicServices basicServices, ServiceAccounts serviceAccounts, CurrentUser currentUser) {
        this.transactions = transactions;
        this.accounts = accounts;
        this.basicServices = basicServices;
        this.serviceAccounts = serviceAccounts;
        this.currentUser = currentUser;
    }

    public List<ModelTransactions> getTransactions(String accountNumber) {
        String number = accountNumber == null ? currentUser.accountNumber() : accountNumber;
        serviceAccounts.getAccount(number); // Comprueba que pertenece al usuario autenticado.
        return transactions.findHistoryByAccount(number);
    }

    public ModelTransactions getTransaction(UUID id) {
        var movement = transactions.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaccion no encontrada"));
        return visibleMovement(movement);
    }

    public ModelTransactions getByIdempotencyKey(UUID key) {
        var movement = transactions.findByIdempotencyKey(key).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Clave no encontrada"));
        return visibleMovement(movement);
    }

    private ModelTransactions visibleMovement(ModelTransactions movement) {
        String number = currentUser.accountNumber();
        serviceAccounts.getAccount(number);
        if (!number.equals(movement.getSourceAccountNumber())
                && !number.equals(movement.getDestinationAccountNumber())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El movimiento no pertenece a tu cuenta");
        }
        return movement;
    }

    public List<ModelBasicServices> getServices() {
        return basicServices.findAllByOrderByCode();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class, timeout = 5)
    public CreationResult deposit(@NotNull UUID key, @NotNull @Valid CashRequest request) {
        return moveMoney(key, null, request.accountNumber(), request.amount(),
                "deposit", request.description(), null, null);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class, timeout = 5)
    public CreationResult withdraw(@NotNull UUID key, @NotNull @Valid CashRequest request) {
        return moveMoney(key, request.accountNumber(), null, request.amount(),
                "withdraw", request.description(), null, null);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class, timeout = 5)
    public CreationResult transfer(@NotNull UUID key, @NotNull @Valid TransferRequest request) {
        return moveMoney(key, currentUser.accountNumber(), request.destinationAccountNumber(), request.amount(),
                "transfer", request.description(), null, null);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class, timeout = 5)
    public CreationResult payService(@NotNull UUID key, @NotNull @Valid ServicePaymentRequest request) {
        var service = basicServices.findById(request.serviceCode()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Servicio no encontrado"));
        return moveMoney(key, currentUser.accountNumber(), service.getAccountNumber(), request.amount(),
                "service_payment", request.description(), service.getCode(), request.customerReference().trim());
    }

    private CreationResult moveMoney(UUID key, String sourceNumber, String destinationNumber,
            BigDecimal requestedAmount, String type, String description, String serviceCode, String reference) {
        BigDecimal amount;
        try {
            amount = requestedAmount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se permiten fracciones de centavo");
        }
        description = description == null || description.isBlank() ? null : description.trim();
        String ownNumber = sourceNumber == null ? destinationNumber : sourceNumber;
        if (!ownNumber.equals(currentUser.accountNumber())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes operar tu propia cuenta");
        }
        if (sourceNumber != null && sourceNumber.equals(destinationNumber)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las cuentas deben ser diferentes");
        }

        var previous = transactions.findByIdempotencyKey(key);
        if (previous.isPresent()) {
            serviceAccounts.getAccount(ownNumber);
            return replay(previous.get(), sourceNumber, destinationNumber, amount, type, description, serviceCode, reference);
        }

        // Una cuenta en depositos/retiros; dos cuentas ordenadas en transferencias/pagos.
        // Bloquear antes del INSERT evita convertir bloqueos de FK en bloqueos mutuos.
        ModelAccounts source = null;
        ModelAccounts destination = null;
        if (sourceNumber == null) {
            destination = lockAccount(destinationNumber);
        } else if (destinationNumber == null) {
            source = lockAccount(sourceNumber);
        } else if (sourceNumber.compareTo(destinationNumber) < 0) {
            source = lockAccount(sourceNumber);
            destination = lockAccount(destinationNumber);
        } else {
            destination = lockAccount(destinationNumber);
            source = lockAccount(sourceNumber);
        }
        currentUser.requireOwner(source == null ? destination : source);
        if ("transfer".equals(type) && destination.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usa pago de servicios para una cuenta recaudadora");
        }

        int inserted = transactions.insertOnce(UUID.randomUUID(), currentUser.userId(), sourceNumber,
                destinationNumber, amount, type, description, serviceCode, reference, key);
        var saved = transactions.findByIdempotencyKey(key).orElseThrow();
        if (inserted == 0) {
            return replay(saved, sourceNumber, destinationNumber, amount, type, description, serviceCode, reference);
        }
        if (source != null && source.getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Saldo insuficiente");
        }
        if (destination != null && destination.getBalance().add(amount).compareTo(MAX_BALANCE) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El destino supera el saldo permitido");
        }
        if (source != null) {
            source.setBalance(source.getBalance().subtract(amount));
        }
        if (destination != null) {
            destination.setBalance(destination.getBalance().add(amount));
        }
        // Movimiento, clave y saldos se confirman juntos o se revierten juntos.
        accounts.flush();
        return new CreationResult(saved, true);
    }

    private ModelAccounts lockAccount(String number) {
        return accounts.findForUpdate(number).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada: " + number));
    }

    private CreationResult replay(ModelTransactions saved, String source, String destination, BigDecimal amount,
            String type, String description, String serviceCode, String reference) {
        if (!saved.getUserId().equals(currentUser.userId())
                || !Objects.equals(saved.getSourceAccountNumber(), source)
                || !Objects.equals(saved.getDestinationAccountNumber(), destination)
                || saved.getAmount().compareTo(amount) != 0 || !saved.getType().equals(type)
                || !Objects.equals(saved.getDescription(), description)
                || !Objects.equals(saved.getServiceCode(), serviceCode)
                || !Objects.equals(saved.getCustomerReference(), reference)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency-Key ya utilizado con otros datos");
        }
        return new CreationResult(saved, false);
    }

    public record CreationResult(ModelTransactions transaction, boolean created) {
    }
}
