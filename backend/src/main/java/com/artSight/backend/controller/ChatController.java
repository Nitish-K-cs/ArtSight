package com.artSight.backend.controller;

import com.artSight.backend.dto.ArtworkChatReply;
import com.artSight.backend.dto.ChatMessageRequest;
import com.artSight.backend.dto.ChatSessionResponse;
import com.artSight.backend.dto.ChatMessageResponse;
import com.artSight.backend.entity.Artwork;
import com.artSight.backend.model.ChatSession;
import com.artSight.backend.repository.ArtworkRepository;
import com.artSight.backend.service.ChatService;
import com.artSight.backend.service.ConversationMemoryService;
import com.artSight.backend.service.InteractionTrackingService;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Phase 2: Conversational layer for ArtSight.
 *
 * API surface:
 *   POST   /artworks/{id}/chat/session              → create new session, returns starters
 *   POST   /artworks/{id}/chat/session/{sid}/message → send a message, get AI reply
 *   GET    /artworks/{id}/chat/session/{sid}         → retrieve session history
 *   DELETE /artworks/{id}/chat/session/{sid}         → close a session
 */
@RestController
@RequestMapping("/artworks")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private ConversationMemoryService memoryService;

    @Autowired
    private ArtworkRepository artworkRepository;

    @Autowired
    private InteractionTrackingService trackingService;

    // ─────────────────────── Create Session ────────────────────────────────

    /**
     * Start a new conversation about a specific artwork.
     * Returns session ID + Gemini-generated conversation starters.
     */
    @PostMapping("/{id}/chat/session")
    public ResponseEntity<?> createSession(
            @PathVariable Long id,
            Authentication auth) {

        Optional<Artwork> artworkOpt = artworkRepository.findById(id);
        if (artworkOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Artwork artwork = artworkOpt.get();
        String userId = auth.getName();

        ChatSession session = memoryService.createSession(userId, artwork);
        List<String> starters = chatService.generateConversationStarters(artwork);

        // Phase 3: record chat interaction for recommendations
        trackingService.recordChat(userId, artwork);

        ChatSessionResponse response = new ChatSessionResponse();
        response.setSessionId(session.getSessionId());
        response.setArtworkId(artwork.getId());
        response.setArtworkTitle(artwork.getTitle());
        response.setArtworkArtist(artwork.getArtist());
        response.setHistory(List.of());
        response.setSuggestedStarters(starters);

        return ResponseEntity.ok(response);
    }

    // ─────────────────────── Send Message ──────────────────────────────────

    /**
     * Send a message in an existing session.
     * Returns the AI's reply + optional related artworks if the intent is detected.
     */
    @PostMapping("/{id}/chat/session/{sessionId}/message")
    public ResponseEntity<?> sendMessage(
            @PathVariable Long id,
            @PathVariable String sessionId,
            @RequestBody ChatMessageRequest request,
            Authentication auth) {

        Optional<ChatSession> sessionOpt = memoryService.getSession(sessionId);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.status(404)
                    .body(Map.of("error", "Session not found or expired. Please start a new session."));
        }

        ChatSession session = sessionOpt.get();

        // Guard: session must belong to this user and artwork
        if (!session.getUserId().equals(auth.getName()) || !session.getArtworkId().equals(id)) {
            return ResponseEntity.status(403).body(Map.of("error", "Session does not match artwork or user"));
        }

        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message cannot be empty"));
        }

        ArtworkChatReply reply = chatService.chat(session, request.getMessage());
        return ResponseEntity.ok(reply);
    }

    // ─────────────────────── Get Session History ───────────────────────────

    /**
     * Retrieve the full conversation history for a session.
     * Useful for restoring state when re-opening a chat in the UI.
     */
    @GetMapping("/{id}/chat/session/{sessionId}")
    public ResponseEntity<?> getSession(
            @PathVariable Long id,
            @PathVariable String sessionId,
            Authentication auth) {

        Optional<ChatSession> sessionOpt = memoryService.getSession(sessionId);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Session not found or expired"));
        }

        ChatSession session = sessionOpt.get();

        if (!session.getUserId().equals(auth.getName()) || !session.getArtworkId().equals(id)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        // Map Spring AI Message types back to simple role/content pairs
        List<ChatMessageResponse> history = session.getHistory().stream()
                .map(msg -> {
                    String role = (msg instanceof UserMessage) ? "user" : "assistant";
                    return new ChatMessageResponse(role, msg.getText());
                })
                .collect(Collectors.toList());

        ChatSessionResponse response = new ChatSessionResponse();
        response.setSessionId(session.getSessionId());
        response.setArtworkId(session.getArtworkId());
        response.setArtworkTitle(session.getArtwork().getTitle());
        response.setArtworkArtist(session.getArtwork().getArtist());
        response.setHistory(history);
        response.setSuggestedStarters(List.of()); // starters only on session creation

        return ResponseEntity.ok(response);
    }

    // ─────────────────────── Close Session ─────────────────────────────────

    /**
     * Explicitly close / delete a session when the user is done chatting.
     */
    @DeleteMapping("/{id}/chat/session/{sessionId}")
    public ResponseEntity<?> closeSession(
            @PathVariable Long id,
            @PathVariable String sessionId,
            Authentication auth) {

        Optional<ChatSession> sessionOpt = memoryService.getSession(sessionId);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ChatSession session = sessionOpt.get();
        if (!session.getUserId().equals(auth.getName())) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        memoryService.deleteSession(sessionId);
        return ResponseEntity.ok(Map.of("message", "Session closed"));
    }
}