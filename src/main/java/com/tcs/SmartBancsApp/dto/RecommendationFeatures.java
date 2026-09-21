package com.tcs.SmartBancsApp.dto;

import java.math.BigDecimal;
import java.util.Map;

public record RecommendationFeatures(
        String accountNumber,
        String period,
        BigDecimal income,
        BigDecimal expenses,
        BigDecimal savings,
        BigDecimal expenseRatio,
        int transactionCount,
        Map<String, BigDecimal> categories) {
}
