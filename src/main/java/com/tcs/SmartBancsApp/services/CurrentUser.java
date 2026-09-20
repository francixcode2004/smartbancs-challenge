package com.tcs.SmartBancsApp.services;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import com.tcs.SmartBancsApp.model.ModelAccounts;

@Component
public class CurrentUser {
    private Jwt token() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sesion primero");
        }
        return jwt;
    }

    public UUID userId() {
        return UUID.fromString(token().getSubject());
    }

    public String accountNumber() {
        return token().getClaimAsString("account_number");
    }

    public void requireOwner(ModelAccounts account) {
        if (!userId().equals(account.getUserId()) || !account.getAccountNumber().equals(accountNumber())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para operar esta cuenta");
        }
    }
}
