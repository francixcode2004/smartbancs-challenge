package com.tcs.SmartBancsApp.repositories;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import com.tcs.SmartBancsApp.model.ModelBancsOutbox;

public interface RepositoryBancsOutbox extends JpaRepository<ModelBancsOutbox, UUID> {
    long countByStatus(String status);

    @Query(value = """
            SELECT * FROM bancs_outbox
            WHERE status = 'PENDING' AND available_at <= CURRENT_TIMESTAMP
            ORDER BY created_at, event_id
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<ModelBancsOutbox> claimPending(@Param("limit") int limit);

    ModelBancsOutbox findTopByStatusAndProcessedAtNotNullOrderByProcessedAtDesc(String status);
}
