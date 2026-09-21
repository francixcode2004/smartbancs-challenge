package com.tcs.SmartBancsApp.services;

import java.time.Instant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;
import com.tcs.SmartBancsApp.config.SecurityConfig;
import com.tcs.SmartBancsApp.config.CredentialVersion;
import com.tcs.SmartBancsApp.dto.PasswordChangeRequest;
import com.tcs.SmartBancsApp.dto.LoginRequest;
import com.tcs.SmartBancsApp.repositories.RepositoryAccounts;
import com.tcs.SmartBancsApp.repositories.RepositoryUsers;

@Service
@Validated
public class ServiceAuth {
    private final RepositoryAccounts accounts;
    private final RepositoryUsers users;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;
    private final CurrentUser currentUser;

    public ServiceAuth(RepositoryAccounts accounts, RepositoryUsers users,
            PasswordEncoder passwords, JwtEncoder encoder, CurrentUser currentUser) {
        this.accounts = accounts;
        this.users = users;
        this.passwords = passwords;
        this.encoder = encoder;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public TokenResponse login(@NotNull @Valid LoginRequest request) {
        var user = users.findByEmailIgnoreCase(request.email().trim()).orElseThrow(this::invalidCredentials);
        var account = accounts.findByUserIdOrderByAccountNumber(user.getUserId()).stream().findFirst()
                .orElseThrow(this::invalidCredentials);
        boolean matches;
        try {
            matches = passwords.matches(request.password(), user.getPassword());
        } catch (IllegalArgumentException exception) {
            // Incluye los usuarios semilla cuyo password es DEMO_LOGIN_DISABLED.
            matches = false;
        }
        if (!matches) {
            throw invalidCredentials();
        }
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer(SecurityConfig.ISSUER)
                .subject(user.getUserId().toString()).issuedAt(now).expiresAt(now.plusSeconds(900))
                .claim("account_number", account.getAccountNumber())
                .claim("credentials", CredentialVersion.of(user.getPassword())).build();
        var headers = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", 900, account.getAccountNumber());
    }

    @Transactional
    public void changePassword(@NotNull @Valid PasswordChangeRequest request) {
        var user = users.findByEmailIgnoreCase(request.email().trim()).orElseThrow(this::invalidCredentials);
        var account = accounts.findByUserIdOrderByAccountNumber(user.getUserId()).stream().findFirst()
                .orElseThrow(this::invalidCredentials);
        // La cuenta enviada nunca puede cambiar la identidad del JWT.
        if (!currentUser.accountNumber().equals(account.getAccountNumber())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes cambiar tu propia contrasena");
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las nuevas contrasenas no coinciden");
        }
        currentUser.requireOwner(account);
        user = users.findForUpdate(account.getUserId()).orElseThrow(this::invalidCredentials);
        boolean matches;
        try {
            matches = passwords.matches(request.currentPassword(), user.getPassword());
        } catch (IllegalArgumentException exception) {
            matches = false;
        }
        if (!matches) throw invalidCredentials();
        if (request.newPassword().equals(request.currentPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usa una contrasena diferente a la anterior");
        }
        user.setPassword(passwords.encode(request.newPassword()));
        users.flush(); // La nueva huella invalida los tokens anteriores.
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cuenta o contrasena incorrecta");
    }

    public record TokenResponse(String accessToken, String tokenType, long expiresIn, String accountNumber) {
        @Override
        public String toString() {
            return "TokenResponse[accessToken=REDACTED]";
        }
    }
}
