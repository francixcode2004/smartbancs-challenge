package com.tcs.SmartBancsApp.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import com.tcs.SmartBancsApp.model.ModelAccounts;

public interface RepositoryAccounts extends JpaRepository<ModelAccounts, String> {
    List<ModelAccounts> findByUserIdOrderByAccountNumber(UUID userId);
    List<ModelAccounts> findByUserIdIsNotNullOrderByAccountNumber();
    boolean existsByUserId(UUID userId);

    @Query(value = "SELECT nextval('account_number_seq')::text", nativeQuery = true)
    String nextAccountNumber();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM ModelAccounts a WHERE a.accountNumber = :number")
    Optional<ModelAccounts> findForUpdate(@Param("number") String number);
}
