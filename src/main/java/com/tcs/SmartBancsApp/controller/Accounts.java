package com.tcs.SmartBancsApp.controller;

import java.util.List;
import org.springframework.web.bind.annotation.*;
import com.tcs.SmartBancsApp.model.ModelAccounts;
import com.tcs.SmartBancsApp.services.ServiceAccounts;

@RestController
@RequestMapping("/accounts")
public class Accounts {
    private final ServiceAccounts serviceAccounts;

    public Accounts(ServiceAccounts serviceAccounts) {
        this.serviceAccounts = serviceAccounts;
    }

    @GetMapping({"", "/"})
    public List<ModelAccounts> getAccounts() {
        return serviceAccounts.getAccounts();
    }

    @GetMapping("/{number}/recipient")
    public ServiceAccounts.RecipientResponse getRecipient(@PathVariable("number") String number) {
        return serviceAccounts.getRecipient(number);
    }

    @GetMapping("/{number}")
    public ModelAccounts getAccount(@PathVariable("number") String number) {
        return serviceAccounts.getAccount(number);
    }
}
