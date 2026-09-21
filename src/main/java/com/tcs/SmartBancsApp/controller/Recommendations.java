package com.tcs.SmartBancsApp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import com.tcs.SmartBancsApp.dto.RecommendationMovement;
import com.tcs.SmartBancsApp.model.ModelRecommendation;
import com.tcs.SmartBancsApp.services.CurrentUser;
import com.tcs.SmartBancsApp.services.RecommendationService;

@RestController
@RequestMapping("/recommendations")
public class Recommendations {
    private final RecommendationService recommendations;
    private final CurrentUser currentUser;

    public Recommendations(RecommendationService recommendations, CurrentUser currentUser) {
        this.recommendations = recommendations;
        this.currentUser = currentUser;
    }

    @GetMapping
    public ResponseEntity<ModelRecommendation> latest() {
        var recommendation = recommendations.latest(currentUser.accountNumber());
        return recommendation == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(recommendation);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(@org.springframework.web.bind.annotation.RequestBody List<RecommendationMovement> movements) {
        recommendations.refreshAsync(currentUser.accountNumber(), movements);
        return ResponseEntity.accepted().build();
    }
}
