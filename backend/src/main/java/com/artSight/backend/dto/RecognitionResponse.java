package com.artSight.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecognitionResponse {
    private boolean matched;

    @JsonProperty ("artwork_id")
    private Long artworkId;
    private Double score;
    private String message; // present only when matched = false
}