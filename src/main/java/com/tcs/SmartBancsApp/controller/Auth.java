package com.tcs.SmartBancsApp.controller;

import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.tcs.SmartBancsApp.dto.LoginRequest;
import com.tcs.SmartBancsApp.dto.PasswordChangeRequest;
import com.tcs.SmartBancsApp.services.ServiceAuth;

@RestController
@RequestMapping("/auth")
public class Auth {
    private final ServiceAuth service;

    public Auth(ServiceAuth service) {
        this.service = service;
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        service.changePassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<ServiceAuth.TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.login(request));
    }
}
