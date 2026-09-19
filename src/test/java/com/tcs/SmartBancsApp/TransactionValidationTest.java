package com.tcs.SmartBancsApp;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.UUID;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import com.tcs.SmartBancsApp.dto.TransactionRequest;

class TransactionValidationTest {
    @Test
    void acceptsExactCentAmountsAndMaximum() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            for (String amount : new String[]{"0.01", "0.29", "12345678.91", "99999999.99"}) {
                assertTrue(validator.validate(new TransactionRequest(
                        UUID.randomUUID(), new BigDecimal(amount), "credit")).isEmpty(), amount);
            }
        }
    }

    @Test
    void rejectsFractionalCentsNegativeZeroAndOverflow() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            for (String amount : new String[]{"10.005", "-0.01", "0", "100000000.00"}) {
                assertFalse(validator.validate(new TransactionRequest(
                        UUID.randomUUID(), new BigDecimal(amount), "debit")).isEmpty(), amount);
            }
        }
    }

    @Test
    void rejectsMissingFieldsAndUnsupportedType() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertFalse(validator.validate(new TransactionRequest(null, null, null)).isEmpty());
            assertFalse(validator.validate(new TransactionRequest(
                    UUID.randomUUID(), new BigDecimal("1.00"), "transfer")).isEmpty());
        }
    }
}
