package com.tcs.SmartBancsApp.services;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.tcs.SmartBancsApp.model.ModelAccounts;
import com.tcs.SmartBancsApp.repositories.RepositoryAccounts;
import com.tcs.SmartBancsApp.repositories.RepositoryUsers;

@Service
@Transactional(readOnly = true)
public class ServiceAccounts {
    private final RepositoryAccounts accounts;
    private final CurrentUser currentUser;
    private final RepositoryUsers users;

    public ServiceAccounts(RepositoryAccounts accounts, CurrentUser currentUser, RepositoryUsers users) {
        this.accounts = accounts;
        this.currentUser = currentUser;
        this.users = users;
    }

    public RecipientResponse getRecipient(String number) {
        if (!number.matches("[0-9]{8}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cuenta debe tener ocho digitos");
        }
        var account = accounts.findById(number).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada"));
        if (account.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esta cuenta corresponde a un servicio");
        }
        var user = users.findById(account.getUserId()).orElseThrow();
        return new RecipientResponse(account.getAccountNumber(), user.getName());
    }

    public record RecipientResponse(String accountNumber, String name) {}

    public List<ModelAccounts> getAccounts() {
        return List.of(getAccount(currentUser.accountNumber()));
    }

    public ModelAccounts getAccount(String number) {
        var account = accounts.findById(number).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada"));
        currentUser.requireOwner(account);
        return account;
    }
}
