package com.tcs.SmartBancsApp.repositories;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import com.tcs.SmartBancsApp.model.ModelTransactions;

public interface RepositoryTransactions extends JpaRepository<ModelTransactions, UUID> {
    Optional<ModelTransactions> findByIdempotencyKey(UUID idempotencyKey);
    boolean existsByUserId(UUID userId);
    boolean existsBySourceAccountNumberOrDestinationAccountNumber(String source, String destination);

    @Query(value = """
            SELECT * FROM transactions
            WHERE source_account_number = :number OR destination_account_number = :number
            ORDER BY created_at DESC, id DESC
            """, nativeQuery = true)
    List<ModelTransactions> findHistoryByAccount(@Param("number") String number);

    @Modifying
    @Query(value = """
            INSERT INTO transactions
                (id, user_id, source_account_number, destination_account_number,
                 amount, type, description, service_code, customer_reference, idempotency_key, created_at)
            VALUES (:id, :userId, :source, :destination, :amount, :type,
                    :description, :serviceCode, :reference, :key, CURRENT_TIMESTAMP)
            ON CONFLICT (idempotency_key) DO NOTHING
            """, nativeQuery = true)
    int insertOnce(@Param("id") UUID id, @Param("userId") UUID userId,
            @Param("source") String source, @Param("destination") String destination,
            @Param("amount") BigDecimal amount, @Param("type") String type,
            @Param("description") String description,
            @Param("serviceCode") String serviceCode, @Param("reference") String reference,
            @Param("key") UUID key);
}
