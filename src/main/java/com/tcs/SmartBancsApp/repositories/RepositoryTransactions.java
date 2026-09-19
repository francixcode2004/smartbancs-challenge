package com.tcs.SmartBancsApp.repositories;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tcs.SmartBancsApp.model.ModelTransactions;

public interface RepositoryTransactions extends JpaRepository<ModelTransactions, UUID> {

    Optional<ModelTransactions> findByIdempotencyKey(UUID idempotencyKey);

    boolean existsByUserId(UUID userId);

    List<ModelTransactions> findAllByOrderByCreatedAtDescTransactionIdDesc();

    List<ModelTransactions> findByUserIdOrderByCreatedAtDescTransactionIdDesc(UUID userId);

    @Modifying
    @Query(value = """
            INSERT INTO transactions (id, user_id, amount, type, idempotency_key, created_at)
            VALUES (:id, :userId, :amount, :type, :key, timezone('UTC', CURRENT_TIMESTAMP))
            ON CONFLICT (idempotency_key) DO NOTHING
            """, nativeQuery = true)
    int insertOnce(
            @Param("id") UUID id,
            @Param("userId") UUID userId,
            @Param("amount") BigDecimal amount,
            @Param("type") String type,
            @Param("key") UUID key);
}