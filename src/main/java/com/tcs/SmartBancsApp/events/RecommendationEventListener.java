package com.tcs.SmartBancsApp.events;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import com.tcs.SmartBancsApp.services.RecommendationService;

@Component
public class RecommendationEventListener {
    private final RecommendationService recommendations;

    public RecommendationEventListener(RecommendationService recommendations) {
        this.recommendations = recommendations;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransactionCommitted(TransactionCommittedEvent event) {
        recommendations.refreshAsync(event.accountNumber());
    }
}
