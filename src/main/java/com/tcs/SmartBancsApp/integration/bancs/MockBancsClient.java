package com.tcs.SmartBancsApp.integration.bancs;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MockBancsClient implements BancsClient {
    private final boolean fail;

    public MockBancsClient(@Value("${bancs.mock.fail:false}") boolean fail) {
        this.fail = fail;
    }

    @Override
    public void sendBatch(List<BancsTransactionEvent> events) {
        if (fail) {
            throw new IllegalStateException("Bancs mock no disponible");
        }
        // Simula una entrega idempotente al core legado sin realizar una llamada externa.
    }
}
