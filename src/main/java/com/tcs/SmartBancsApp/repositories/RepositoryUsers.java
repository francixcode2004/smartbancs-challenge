package com.tcs.SmartBancsApp.repositories;

import java.util.UUID;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tcs.SmartBancsApp.model.ModelUsers;

public interface RepositoryUsers extends JpaRepository<ModelUsers, UUID> {
    Optional<ModelUsers> findByEmailIgnoreCase(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM ModelUsers u WHERE u.userId = :id")
    Optional<ModelUsers> findForUpdate(@Param("id") UUID id);
}
