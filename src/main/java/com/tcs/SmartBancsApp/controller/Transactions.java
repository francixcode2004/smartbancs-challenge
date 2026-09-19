package com.tcs.SmartBancsApp.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.tcs.SmartBancsApp.dto.TransactionRequest;
import com.tcs.SmartBancsApp.model.ModelTransactions;
import com.tcs.SmartBancsApp.services.ServiceTransactions;

@RestController
@RequestMapping("/transactions")
public class Transactions {

    private final ServiceTransactions serviceTransactions;

    public Transactions(ServiceTransactions serviceTransactions) {
        this.serviceTransactions = serviceTransactions;
    }

    @GetMapping({"", "/"})
    public List<ModelTransactions> getTransactions(
            @RequestParam(name = "userId", required = false) UUID userId) {
        return serviceTransactions.getTransactions(userId);
    }

    @GetMapping("/{id}")
    public ModelTransactions getTransaction(@PathVariable("id") UUID id) {
        return serviceTransactions.getTransaction(id);
    }

    @GetMapping("/by-key/{key}")
    public ModelTransactions getByIdempotencyKey(@PathVariable("key") UUID key) {
        return serviceTransactions.getByIdempotencyKey(key);
    }

    @PostMapping({"", "/"})
    public ResponseEntity<ModelTransactions> createTransaction(
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody TransactionRequest request) {
        var result = serviceTransactions.createTransaction(key, request);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .location(URI.create("/transactions/" + result.transaction().getTransactionId()))
                .header("Idempotency-Replayed", Boolean.toString(!result.created()))
                .body(result.transaction());
    }
}