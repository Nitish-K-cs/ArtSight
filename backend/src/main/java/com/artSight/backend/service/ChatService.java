package com.artSight.backend.service;

import com.artSight.backend.dto.ArtworkChatReply;
import com.artSight.backend.entity.Artwork;
import com.artSight.backend.model.ChatSession;
import com.artSight.backend.repository.ArtworkRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Core conversational service for ArtSight Phase 2.
 *
 * Responsibilities:
 *  1. Build a rich system prompt from Artwork metadata.
 *  2. Execute multi-turn conversations using Spring AI ChatClient.
 *  3. Generate conversation starters tailored to a specific artwork.
 *  4. Detect "suggest related" intent and surface related artworks from DB.
 */
@Service
public class ChatService {

    private final ChatClient chatClient;
    private final ArtworkRepository artworkRepository;

    // Keywords that signal the user wants related artwork suggestions
    private static final List<String> RELATED_INTENT_KEYWORDS = List.of(
            "similar", "related", "influenced", "other works", "same period",
            "same artist", "same style", "more like", "other paintings", "influenced by"
    );

    @Autowired
    public ChatService(ChatClient.Builder chatClientBuilder, ArtworkRepository artworkRepository) {
        this.chatClient = chatClientBuilder.build();
        this.artworkRepository = artworkRepository;
    }

    // ───────────────────────── Main Chat Method ─────────────────────────────

    /**
     * Send a user message in an existing session and get back an AI reply.
     * History is preserved across calls for multi-turn conversation.
     *
     * @param session    the active ChatSession (with history)
     * @param userMessage the user's latest message
     * @return ArtworkChatReply with the assistant reply + optional related artworks
     */
    public ArtworkChatReply chat(ChatSession session, String userMessage) {
        // 1. Add user message to history
        session.addUserMessage(userMessage);

        // 2. Build the full prompt: system + history
        String systemPrompt = buildSystemPrompt(session.getArtwork());
        List<Message> history = session.getHistory();

        String reply = chatClient.prompt()
                .system(systemPrompt)
                .messages(history)
                .call()
                .content();

        // 3. Add assistant reply to history
        session.addAssistantMessage(reply);

        // 4. Build response — check if we should surface related artworks
        ArtworkChatReply response = new ArtworkChatReply();
        response.setReply(reply);

        if (detectRelatedArtworkIntent(userMessage)) {
            List<ArtworkChatReply.RelatedArtwork> related = findRelatedArtworks(session.getArtwork());
            response.setRelatedArtworks(related);
        }

        return response;
    }

    // ─────────────────────── Conversation Starters ──────────────────────────

    /**
     * Ask Gemini to generate 5 tailored conversation starters for this artwork.
     * These are shown to the user as quick-tap prompts in the UI.
     */
    public List<String> generateConversationStarters(Artwork artwork) {
        String prompt = String.format(
                """
                You are an art museum guide. Generate exactly 5 short, engaging conversation starter
                questions a museum visitor might ask about this specific artwork. Make them specific
                to the artwork's details, not generic.
                
                Artwork: "%s" by %s (%s)
                Medium: %s | Culture: %s | Department: %s
                
                Return ONLY the 5 questions, one per line, no numbering, no extra text.
                """,
                artwork.getTitle(), artwork.getArtist(), artwork.getDateCreated(),
                artwork.getMedium(), artwork.getCulture(), artwork.getDepartment()
        );

        String raw = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        // Parse newline-separated starters, trim blank lines
        return raw.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .limit(5)
                .collect(Collectors.toList());
    }

    // ───────────────────────── Helper Methods ───────────────────────────────

    /**
     * Builds a detailed system prompt that grounds Gemini in this specific artwork's context.
     * A rich system prompt is the key to accurate, engaging art historical responses.
     */
    private String buildSystemPrompt(Artwork artwork) {
        return String.format(
                """
                You are ArtSight, an expert AI art historian and museum guide. You are currently
                helping a visitor learn about the following artwork:
                
                Title: "%s"
                Artist: %s
                Date Created: %s
                Medium: %s
                Culture: %s
                Department: %s
                Description: %s
                
                Guidelines:
                - Stay focused on this artwork and its historical/cultural context.
                - When the user asks about the artist's other works or influences, provide real,
                  accurate information and let them know they can explore more in the app.
                - If asked about something outside art history, gently redirect to the artwork.
                - Be engaging, informative, and accessible. Avoid overly academic language.
                - Answers should be concise (2–4 paragraphs max) unless the user asks for depth.
                - If mentioning other artworks or artists, be factually accurate.
                """,
                artwork.getTitle(),
                artwork.getArtist(),
                artwork.getDateCreated(),
                artwork.getMedium(),
                artwork.getCulture(),
                artwork.getDepartment(),
                artwork.getDescription() != null ? artwork.getDescription() : "No description available."
        );
    }

    /**
     * Keyword-based intent detection for "show me related artworks" requests.
     * Lightweight and zero-latency — no extra LLM call needed.
     */
    private boolean detectRelatedArtworkIntent(String message) {
        String lower = message.toLowerCase();
        return RELATED_INTENT_KEYWORDS.stream().anyMatch(lower::contains);
    }

    /**
     * Finds related artworks from the same department and culture.
     * Excludes the current artwork and limits to 5 results.
     */
    private List<ArtworkChatReply.RelatedArtwork> findRelatedArtworks(Artwork current) {
        List<Artwork> candidates = artworkRepository
                .findTop10ByDepartmentAndIdNot(current.getDepartment(), current.getId());

        if (candidates.isEmpty()) {
            candidates = artworkRepository
                    .findTop10ByCultureAndIdNot(current.getCulture(), current.getId());
        }

        return candidates.stream()
                .limit(5)
                .map(a -> new ArtworkChatReply.RelatedArtwork(a,
                        "Same department: " + current.getDepartment()))
                .collect(Collectors.toList());
    }
}