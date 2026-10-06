package com.artSight.backend.dto;

import lombok.Data;

import java.util.List;

@Data
public class ChatSessionResponse {
    private String sessionId;
    private Long artworkId;
    private String artworkTitle;
    private String artworkArtist;
    private List<ChatMessageResponse> history;
    private List<String> suggestedStarters;
}
