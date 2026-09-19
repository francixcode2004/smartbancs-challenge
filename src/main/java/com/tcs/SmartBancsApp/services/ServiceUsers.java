package com.tcs.SmartBancsApp.services;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import com.tcs.SmartBancsApp.dto.UserRequest;
import com.tcs.SmartBancsApp.model.ModelUsers;
import com.tcs.SmartBancsApp.repositories.RepositoryTransactions;
import com.tcs.SmartBancsApp.repositories.RepositoryUsers;

@Service
@Validated
@Transactional(readOnly = true)
public class ServiceUsers {

    private final RepositoryUsers repositoryUsers;
    private final RepositoryTransactions repositoryTransactions;
    private final PasswordEncoder passwordEncoder =
            Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();

    public ServiceUsers(RepositoryUsers repositoryUsers,
            RepositoryTransactions repositoryTransactions) {
        this.repositoryUsers = repositoryUsers;
        this.repositoryTransactions = repositoryTransactions;
    }

    public List<ModelUsers> getUsers() {
        return repositoryUsers.findAll();
    }

    public ModelUsers getUser(UUID id) {
        return repositoryUsers.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    @Transactional
    public ModelUsers createUser(@NotNull @Valid UserRequest request) {
        ModelUsers user = new ModelUsers();
        applyChanges(user, request);
        return repositoryUsers.saveAndFlush(user);
    }

    @Transactional
    public ModelUsers updateUser(UUID id, @NotNull @Valid UserRequest request) {
        ModelUsers user = getUser(id);
        applyChanges(user, request);
        return repositoryUsers.saveAndFlush(user);
    }

    @Transactional
    public void deleteUser(UUID id) {
        ModelUsers user = getUser(id);
        if (repositoryTransactions.existsByUserId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar un usuario con movimientos registrados");
        }
        // The RESTRICT foreign key also protects against concurrent inserts.
        repositoryUsers.delete(user);
        repositoryUsers.flush();
    }

    private void applyChanges(ModelUsers user, UserRequest request) {
        user.setName(request.name().trim());
        user.setEmail(request.email().trim());
        user.setPassword(passwordEncoder.encode(request.password()));
    }
}