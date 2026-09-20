package com.tcs.SmartBancsApp.controller;

import java.net.URI;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.tcs.SmartBancsApp.dto.UserRequest;
import com.tcs.SmartBancsApp.dto.UserResponse;
import com.tcs.SmartBancsApp.dto.ProfileRequest;
import com.tcs.SmartBancsApp.model.ModelTransactions;
import com.tcs.SmartBancsApp.services.ServiceUsers;
import com.tcs.SmartBancsApp.services.ServiceTransactions;

@RestController
@RequestMapping("/users")
public class Users {
    private final ServiceUsers serviceUsers;
    private final ServiceTransactions transactions;

    public Users(ServiceUsers serviceUsers, ServiceTransactions transactions) {
        this.serviceUsers = serviceUsers;
        this.transactions = transactions;
    }

    @GetMapping({"", "/"})
    public List<UserResponse> getUsers() {
        return serviceUsers.getUsers();
    }

    @GetMapping("/{accountNumber}")
    public UserResponse getUser(@PathVariable("accountNumber") String accountNumber) {
        return serviceUsers.getUser(accountNumber);
    }

    @GetMapping("/{accountNumber}/transactions")
    public List<ModelTransactions> getMovements(@PathVariable("accountNumber") String accountNumber) {
        return transactions.getTransactions(accountNumber);
    }

    @PostMapping({"", "/"})
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        var user = serviceUsers.createUser(request);
        return ResponseEntity.created(URI.create("/users/" + user.accountNumber())).body(user);
    }

    @PutMapping("/{accountNumber}")
    public UserResponse updateUser(@PathVariable("accountNumber") String accountNumber,
            @Valid @RequestBody ProfileRequest request) {
        return serviceUsers.updateUser(accountNumber, request);
    }

    @DeleteMapping("/{accountNumber}")
    public ResponseEntity<Void> deleteUser(@PathVariable("accountNumber") String accountNumber) {
        serviceUsers.deleteUser(accountNumber);
        return ResponseEntity.noContent().build();
    }
}
