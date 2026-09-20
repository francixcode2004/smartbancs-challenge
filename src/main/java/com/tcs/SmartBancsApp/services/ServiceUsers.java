package com.tcs.SmartBancsApp.services;

import java.util.List;
import java.util.Locale;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;
import com.tcs.SmartBancsApp.dto.UserRequest;
import com.tcs.SmartBancsApp.dto.UserResponse;
import com.tcs.SmartBancsApp.dto.ProfileRequest;
import com.tcs.SmartBancsApp.model.ModelUsers;
import com.tcs.SmartBancsApp.model.ModelAccounts;
import com.tcs.SmartBancsApp.repositories.*;

@Service
@Validated
@Transactional(readOnly = true)
public class ServiceUsers {
    private final RepositoryUsers users;
    private final RepositoryAccounts accounts;
    private final RepositoryTransactions transactions;
    private final ServiceAccounts serviceAccounts;
    private final CurrentUser currentUser;
    private final PasswordEncoder passwords;

    public ServiceUsers(RepositoryUsers users, RepositoryAccounts accounts,
            RepositoryTransactions transactions, ServiceAccounts serviceAccounts,
            CurrentUser currentUser, PasswordEncoder passwords) {
        this.users = users;
        this.accounts = accounts;
        this.transactions = transactions;
        this.serviceAccounts = serviceAccounts;
        this.currentUser = currentUser;
        this.passwords = passwords;
    }

    // Un cliente solo puede consultar su perfil; no hay rol de administrador en esta demo.
    public List<UserResponse> getUsers() {
        return List.of(getUser(currentUser.accountNumber()));
    }

    public UserResponse getUser(String number) {
        var account = serviceAccounts.getAccount(number);
        return response(users.findById(account.getUserId()).orElseThrow(), account);
    }

    @Transactional
    public UserResponse createUser(@NotNull @Valid UserRequest request) {
        var user = new ModelUsers();
        applyChanges(user, request);
        user = users.saveAndFlush(user);
        var account = new ModelAccounts();
        account.setAccountNumber(accounts.nextAccountNumber());
        account.setUserId(user.getUserId());
        account = accounts.saveAndFlush(account);
        return response(user, account);
    }

    @Transactional
    public UserResponse updateUser(String number, @NotNull @Valid ProfileRequest request) {
        var account = serviceAccounts.getAccount(number);
        var user = users.findForUpdate(account.getUserId()).orElseThrow();
        user.setName(request.name().trim());
        user.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        return response(users.saveAndFlush(user), account);
    }

    @Transactional
    public void deleteUser(String number) {
        var account = accounts.findForUpdate(number).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada"));
        currentUser.requireOwner(account);
        if (account.getBalance().signum() != 0 || transactions.existsByUserId(account.getUserId())
                || transactions.existsBySourceAccountNumberOrDestinationAccountNumber(number, number)
                || accounts.findByUserIdOrderByAccountNumber(account.getUserId()).size() != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede eliminar un usuario con una unica cuenta sin saldo ni movimientos");
        }
        accounts.delete(account);
        accounts.flush();
        users.deleteById(account.getUserId());
        users.flush();
    }

    private void applyChanges(ModelUsers user, UserRequest request) {
        user.setName(request.name().trim());
        user.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        user.setPassword(passwords.encode(request.password()));
    }

    private UserResponse response(ModelUsers user, ModelAccounts account) {
        return new UserResponse(account.getAccountNumber(), user.getName(), user.getEmail(),
                account.getBalance(), user.getCreatedAt());
    }
}
