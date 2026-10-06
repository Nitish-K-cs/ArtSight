package com.artSight.backend.model;

import com.artSight.backend.entity.Artwork;
import lombok.Data;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * In-memory representation of a single user ↔ artwork conversation session.
 * Spring AI's Message list is used directly so it can be passed straight
 * into ChatClient.prompt().messages().
 */
@Data
public class ChatSession {

    private final String sessionId;
    private final Long artworkId;
    private final String userId;
    private final Artwork artwork;
    private final Instant createdAt = Instant.now();
    private Instant lastAccessedAt = Instant.now();

    /**
     * Full message history in Spring AI format.
     * Starts empty — the system prompt is injected at call-time, not stored here,
     * so it always reflects the latest artwork context.
     */
    private final List<Message> history = new ArrayList<>();

    public ChatSession(String sessionId, Long artworkId, String userId, Artwork artwork) {
        this.sessionId = sessionId;
        this.artworkId = artworkId;
        this.userId = userId;
        this.artwork = artwork;
    }

    /** Add a user turn to history. */
    public void addUserMessage(String content) {
        history.add(new UserMessage(content));
        lastAccessedAt = Instant.now();
    }

    /** Add an assistant turn to history. */
    public void addAssistantMessage(String content) {
        history.add(new AssistantMessage(content));
        lastAccessedAt = Instant.now();
    }
}
