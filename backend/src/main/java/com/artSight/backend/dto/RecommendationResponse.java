package com.artSight.backend.dto;

import lombok.Data;

import java.util.List;

@Data
public class RecommendationResponse {
    private String userEmail;
    private int totalRecommendations;
    private boolean personalised; // false = cold start
    private List<RecommendedArtwork> recommendations;

    @Data
    public static class RecommendedArtwork {
        private Long id;
        private String title;
        private String artist;
        private String dateCreated;
        private String medium;
        private String culture;
        private String department;
        private String imageUrl;
        private String description;
    }
}
