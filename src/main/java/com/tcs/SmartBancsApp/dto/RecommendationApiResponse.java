package com.tcs.SmartBancsApp.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record RecommendationApiResponse(
        List<Item> recommendations,
        String model,
        String promptVersion,
        OffsetDateTime generatedAt,
        String source) {
    public record Item(String title, String message, String priority, String category) {
    }
}
