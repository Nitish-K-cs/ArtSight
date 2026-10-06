package com.artSight.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatMessageResponse {
    private String role;   // "user" or "assistant"
    private String content;
}
