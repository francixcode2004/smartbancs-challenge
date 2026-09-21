package com.tcs.SmartBancsApp.integration.bancs;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcs.SmartBancsApp.model.ModelBancsOutbox;
import com.tcs.SmartBancsApp.model.ModelTransactions;
import com.tcs.SmartBancsApp.repositories.RepositoryBancsOutbox;

@Service
public class BancsOutboxService {
    private static final int BATCH_SIZE = 50;
    private static final int MAX_ATTEMPTS = 5;

    private final RepositoryBancsOutbox outbox;
    private final BancsClient bancsClient;

    public BancsOutboxService(RepositoryBancsOutbox outbox, BancsClient bancsClient) {
        this.outbox = outbox;
        this.bancsClient = bancsClient;
    }

    @Transactional
    public void enqueue(ModelTransactions transaction) {
        outbox.save(new ModelBancsOutbox(transaction));
    }

    @Scheduled(fixedDelayString = "${bancs.sync.fixed-delay-ms:15000}")
    @Transactional
    public void processPendingBatch() {
        List<ModelBancsOutbox> batch = claimPendingBatch();
        if (batch.isEmpty()) {
            return;
        }
        var events = batch.stream().map(this::eventOf).toList();
        try {
            bancsClient.sendBatch(events);
            markSent(batch);
        } catch (RuntimeException exception) {
            markFailure(batch, exception.getMessage());
        }
    }

    @Transactional
    protected List<ModelBancsOutbox> claimPendingBatch() {
        var batch = outbox.claimPending(BATCH_SIZE);
        batch.forEach(event -> event.markProcessing());
        return batch;
    }

    @Transactional
    protected void markSent(List<ModelBancsOutbox> batch) {
        var now = OffsetDateTime.now();
        batch.forEach(event -> event.markSent(now));
        outbox.saveAll(batch);
    }

    @Transactional
    protected void markFailure(List<ModelBancsOutbox> batch, String reason) {
        var now = OffsetDateTime.now();
        String error = reason == null || reason.isBlank() ? "Error desconocido de Bancs" : reason;
        batch.forEach(event -> {
            if (event.getAttempts() >= MAX_ATTEMPTS) {
                event.markFailed(now, error);
            } else {
                long delaySeconds = Math.min(300, 1L << Math.min(event.getAttempts(), 8));
                event.markRetry(now.plusSeconds(delaySeconds), error);
            }
        });
        outbox.saveAll(batch);
    }

    public BancsStatus status() {
        var lastSent = outbox.findTopByStatusAndProcessedAtNotNullOrderByProcessedAtDesc("SENT");
        return new BancsStatus(
                outbox.countByStatus("PENDING"),
                outbox.countByStatus("PROCESSING"),
                outbox.countByStatus("SENT"),
                outbox.countByStatus("FAILED"),
                lastSent == null ? null : lastSent.getProcessedAt());
    }

    private BancsClient.BancsTransactionEvent eventOf(ModelBancsOutbox event) {
        return new BancsClient.BancsTransactionEvent(
                event.getEventId(), event.getTransactionId(), event.getIdempotencyKey(),
                event.getSourceAccountNumber(), event.getDestinationAccountNumber(), event.getAmount(),
                event.getType(), event.getDescription(), event.getServiceCode(), event.getCustomerReference());
    }

    public record BancsStatus(long pendingEvents, long processingEvents, long sentEvents,
            long failedEvents, java.time.OffsetDateTime lastSuccessfulSync) {
    }
}
