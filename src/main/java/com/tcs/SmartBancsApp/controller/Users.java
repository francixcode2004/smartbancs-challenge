package com.tcs.SmartBancsApp.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tcs.SmartBancsApp.dto.UserRequest;
import com.tcs.SmartBancsApp.model.ModelUsers;
import com.tcs.SmartBancsApp.services.ServiceUsers;

@RestController
@RequestMapping("/users")
public class Users {

    private final ServiceUsers serviceUsers;

    public Users(ServiceUsers serviceUsers) {
        this.serviceUsers = serviceUsers;
    }

    @GetMapping({"", "/"})
    public List<ModelUsers> getUsers() {
        return serviceUsers.getUsers();
    }

    @GetMapping("/{id}")
    public ModelUsers getUser(@PathVariable("id") UUID id) {
        return serviceUsers.getUser(id);
    }

    @PostMapping({"", "/"})
    public ResponseEntity<ModelUsers> createUser(@Valid @RequestBody UserRequest request) {
        ModelUsers user = serviceUsers.createUser(request);
        return ResponseEntity.created(URI.create("/users/" + user.getUserId())).body(user);
    }

    @PutMapping("/{id}")
    public ModelUsers updateUser(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UserRequest request) {
        return serviceUsers.updateUser(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") UUID id) {
        serviceUsers.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
