package com.tcs.SmartBancsApp.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.tcs.SmartBancsApp.dto.TransferRequest;
import com.tcs.SmartBancsApp.dto.CashRequest;
import com.tcs.SmartBancsApp.dto.ServicePaymentRequest;
import com.tcs.SmartBancsApp.model.ModelBasicServices;
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
            @RequestParam(name = "accountNumber", required = false) String accountNumber) {
        return serviceTransactions.getTransactions(accountNumber);
    }

    @GetMapping("/{id}")
    public ModelTransactions getTransaction(@PathVariable("id") UUID id) {
        return serviceTransactions.getTransaction(id);
    }

    @GetMapping("/by-key/{key}")
    public ModelTransactions getByIdempotencyKey(@PathVariable("key") UUID key) {
        return serviceTransactions.getByIdempotencyKey(key);
    }

    @GetMapping("/services")
    public List<ModelBasicServices> getServices() {
        return serviceTransactions.getServices();
    }


    @PostMapping("/deposits")
    public ResponseEntity<ModelTransactions> deposit(@RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody CashRequest request) {
        return response(serviceTransactions.deposit(key, request));
    }

    @PostMapping("/withdrawals")
    public ResponseEntity<ModelTransactions> withdraw(@RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody CashRequest request) {
        return response(serviceTransactions.withdraw(key, request));
    }

    @PostMapping("/transfers")
    public ResponseEntity<ModelTransactions> transfer(@RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody TransferRequest request) {
        return response(serviceTransactions.transfer(key, request));
    }

    @PostMapping("/service-payments")
    public ResponseEntity<ModelTransactions> payService(@RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody ServicePaymentRequest request) {
        return response(serviceTransactions.payService(key, request));
    }

    private ResponseEntity<ModelTransactions> response(ServiceTransactions.CreationResult result) {
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .location(URI.create("/transactions/" + result.transaction().getTransactionId()))
                .header("Idempotency-Replayed", Boolean.toString(!result.created()))
                .body(result.transaction());
    }
}
