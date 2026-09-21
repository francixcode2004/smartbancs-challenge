package com.tcs.SmartBancsApp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcs.SmartBancsApp.integration.bancs.BancsOutboxService;

@RestController
@RequestMapping("/integration/bancs")
public class BancsIntegration {
    private final BancsOutboxService outbox;

    public BancsIntegration(BancsOutboxService outbox) {
        this.outbox = outbox;
    }

    @GetMapping("/status")
    public BancsOutboxService.BancsStatus status() {
        return outbox.status();
    }
}
