package com.tcs.SmartBancsApp.repositories;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcs.SmartBancsApp.model.ModelRecommendation;

public interface RepositoryRecommendations extends JpaRepository<ModelRecommendation, UUID> {
    Optional<ModelRecommendation> findTopByAccountNumberOrderByCreatedAtDesc(String accountNumber);
}
