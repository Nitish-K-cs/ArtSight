package com.artSight.backend.controller;

import com.artSight.backend.dto.RecommendationResponse;
import com.artSight.backend.entity.Artwork;
import com.artSight.backend.entity.UserArtworkInteraction;
import com.artSight.backend.repository.UserArtworkInteractionRepository;
import com.artSight.backend.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Phase 3: Personalised recommendation endpoints.
 *
 *   GET /users/me/recommendations  → personalised artwork feed
 *   GET /users/me/history          → what the user has scanned/chatted about
 */
@RestController
@RequestMapping("/users/me")
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private UserArtworkInteractionRepository interactionRepository;

    // ───────────────────── Recommendations ──────────────────────────────────

    /**
     * Returns up to 10 personalised artwork recommendations.
     * If the user has no history yet, returns a diverse cold-start feed.
     */
    @GetMapping("/recommendations")
    public ResponseEntity<RecommendationResponse> getRecommendations(Authentication auth) {
        String email = auth.getName();

        boolean hasHistory = !interactionRepository
                .findByUserEmailOrderByInteractedAtDesc(email).isEmpty();

        List<Artwork> recommended = recommendationService.getRecommendationsForUser(email);

        RecommendationResponse response = new RecommendationResponse();
        response.setUserEmail(email);
        response.setPersonalised(hasHistory);
        response.setTotalRecommendations(recommended.size());
        response.setRecommendations(recommended.stream()
                .map(this::toRecommendedArtwork)
                .collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }

    // ───────────────────── Interaction History ───────────────────────────────

    /**
     * Returns the user's full interaction history (scans + chats), newest first.
     * Useful for a "Recently viewed" section in the frontend.
     */
    @GetMapping("/history")
    public ResponseEntity<?> getHistory(Authentication auth) {
        String email = auth.getName();

        List<UserArtworkInteraction> history =
                interactionRepository.findByUserEmailOrderByInteractedAtDesc(email);

        List<Object> result = history.stream().map(i -> new Object() {
            public final Long artworkId       = i.getArtwork().getId();
            public final String title         = i.getArtwork().getTitle();
            public final String artist        = i.getArtwork().getArtist();
            public final String imageUrl      = i.getArtwork().getImageUrl();
            public final String type          = i.getType().name();
            public final String interactedAt  = i.getInteractedAt().toString();
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // ───────────────────── Helpers ───────────────────────────────────────────

    private RecommendationResponse.RecommendedArtwork toRecommendedArtwork(Artwork a) {
        RecommendationResponse.RecommendedArtwork dto = new RecommendationResponse.RecommendedArtwork();
        dto.setId(a.getId());
        dto.setTitle(a.getTitle());
        dto.setArtist(a.getArtist());
        dto.setDateCreated(a.getDateCreated());
        dto.setMedium(a.getMedium());
        dto.setCulture(a.getCulture());
        dto.setDepartment(a.getDepartment());
        dto.setImageUrl(a.getImageUrl());
        dto.setDescription(a.getDescription());
        return dto;
    }
}
