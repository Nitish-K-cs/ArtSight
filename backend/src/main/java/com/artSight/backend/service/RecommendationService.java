package com.artSight.backend.service;

import com.artSight.backend.entity.Artwork;
import com.artSight.backend.entity.UserArtworkInteraction;
import com.artSight.backend.repository.ArtworkRepository;
import com.artSight.backend.repository.UserArtworkInteractionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Content-based recommendation engine for ArtSight Phase 3.
 *
 * Strategy:
 *  1. Build a taste profile from the user's interaction history
 *     (which artists, cultures, departments they've engaged with).
 *  2. Score every unseen artwork against that profile.
 *  3. Return the top N artworks by score.
 *
 * Scoring weights (tunable):
 *  - Same artist as an interacted artwork:    +3 points
 *  - Same culture:                            +2 points
 *  - Same department:                         +1 point
 *  - CHAT interaction (vs SCAN) multiplier:  ×1.5 (chatting = deeper interest)
 *
 * Fallback: if the user has no history yet, return the most broadly represented
 * artworks (by department diversity) as a "popular picks" cold-start.
 */
@Service
public class RecommendationService {

    private static final int MAX_RECOMMENDATIONS = 10;

    // Scoring weights
    private static final double ARTIST_WEIGHT     = 3.0;
    private static final double CULTURE_WEIGHT    = 2.0;
    private static final double DEPARTMENT_WEIGHT = 1.0;
    private static final double CHAT_MULTIPLIER   = 1.5; // chat counts more than scan

    @Autowired
    private UserArtworkInteractionRepository interactionRepository;

    @Autowired
    private ArtworkRepository artworkRepository;

    // ─────────────────────────── Public API ─────────────────────────────────

    /**
     * Generate personalised recommendations for a user.
     * Returns up to MAX_RECOMMENDATIONS artworks they haven't seen yet.
     */
    public List<Artwork> getRecommendationsForUser(String userEmail) {
        List<UserArtworkInteraction> history =
                interactionRepository.findByUserEmailOrderByInteractedAtDesc(userEmail);

        if (history.isEmpty()) {
            return getColdStartRecommendations();
        }

        // IDs the user has already seen — exclude from recommendations
        Set<Long> seenIds = history.stream()
                .map(i -> i.getArtwork().getId())
                .collect(Collectors.toSet());

        // Build taste profile from history
        TasteProfile profile = buildTasteProfile(history);

        // Score all unseen artworks
        List<Artwork> allArtworks = artworkRepository.findAll();

        return allArtworks.stream()
                .filter(a -> !seenIds.contains(a.getId()))
                .map(a -> Map.entry(a, scoreArtwork(a, profile)))
                .filter(e -> e.getValue() > 0) // must have at least one match
                .sorted(Map.Entry.<Artwork, Double>comparingByValue().reversed())
                .limit(MAX_RECOMMENDATIONS)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    // ─────────────────────────── Taste Profile ───────────────────────────────

    /**
     * Weighted frequency maps of what the user has engaged with.
     * CHAT interactions count 1.5× more than SCAN.
     */
    private TasteProfile buildTasteProfile(List<UserArtworkInteraction> history) {
        Map<String, Double> artistWeights     = new HashMap<>();
        Map<String, Double> cultureWeights    = new HashMap<>();
        Map<String, Double> departmentWeights = new HashMap<>();

        for (UserArtworkInteraction interaction : history) {
            Artwork a = interaction.getArtwork();
            double weight = interaction.getType() == UserArtworkInteraction.InteractionType.CHAT
                    ? CHAT_MULTIPLIER : 1.0;

            if (a.getArtist() != null)
                artistWeights.merge(a.getArtist(), weight, Double::sum);
            if (a.getCulture() != null)
                cultureWeights.merge(a.getCulture(), weight, Double::sum);
            if (a.getDepartment() != null)
                departmentWeights.merge(a.getDepartment(), weight, Double::sum);
        }

        return new TasteProfile(artistWeights, cultureWeights, departmentWeights);
    }

    /**
     * Score an unseen artwork against the user's taste profile.
     * Higher score = better match.
     */
    private double scoreArtwork(Artwork artwork, TasteProfile profile) {
        double score = 0;

        if (artwork.getArtist() != null)
            score += profile.artists().getOrDefault(artwork.getArtist(), 0.0) * ARTIST_WEIGHT;
        if (artwork.getCulture() != null)
            score += profile.cultures().getOrDefault(artwork.getCulture(), 0.0) * CULTURE_WEIGHT;
        if (artwork.getDepartment() != null)
            score += profile.departments().getOrDefault(artwork.getDepartment(), 0.0) * DEPARTMENT_WEIGHT;

        return score;
    }

    // ──────────────────────── Cold-Start Fallback ────────────────────────────

    /**
     * For new users with no history: return a diverse spread of artworks,
     * one from each department, up to MAX_RECOMMENDATIONS.
     */
    private List<Artwork> getColdStartRecommendations() {
        List<Artwork> all = artworkRepository.findAll();

        // Pick one artwork per department for diversity
        Map<String, Artwork> byDepartment = new LinkedHashMap<>();
        for (Artwork a : all) {
            String dept = a.getDepartment() != null ? a.getDepartment() : "Unknown";
            byDepartment.putIfAbsent(dept, a);
            if (byDepartment.size() >= MAX_RECOMMENDATIONS) break;
        }

        List<Artwork> result = new ArrayList<>(byDepartment.values());

        // If we don't have enough departments, fill up with any artworks
        if (result.size() < MAX_RECOMMENDATIONS) {
            Set<Long> picked = result.stream().map(Artwork::getId).collect(Collectors.toSet());
            all.stream()
                    .filter(a -> !picked.contains(a.getId()))
                    .limit(MAX_RECOMMENDATIONS - result.size())
                    .forEach(result::add);
        }

        return result;
    }

    // ─────────────────────────── Inner Types ────────────────────────────────

    private record TasteProfile(
            Map<String, Double> artists,
            Map<String, Double> cultures,
            Map<String, Double> departments
    ) {}
}
