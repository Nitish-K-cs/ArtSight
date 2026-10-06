package com.artSight.backend.service;

import com.artSight.backend.entity.Artwork;
import com.artSight.backend.model.ChatSession;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages in-memory conversation sessions.
 * <p>
 * Sessions are keyed by a random UUID sessionId.
 * A background task evicts sessions idle for more than 2 hours,
 * so memory won't grow unbounded even in long-running deployments.
 */
@Service
public class ConversationMemoryService {

    private static final Duration SESSION_TTL = Duration.ofHours(2);

    /** sessionId → ChatSession */
    private final Map<String, ChatSession> sessions = new ConcurrentHashMap<>();

    // ───────────────────────────── Public API ─────────────────────────────

    /**
     * Create a brand-new session for a user + artwork pair.
     */
    public ChatSession createSession(String userId, Artwork artwork) {
        String sessionId = UUID.randomUUID().toString();
        ChatSession session = new ChatSession(sessionId, artwork.getId(), userId, artwork);
        sessions.put(sessionId, session);
        return session;
    }

    /**
     * Retrieve an existing session, or empty if it doesn't exist / has expired.
     */
    public Optional<ChatSession> getSession(String sessionId) {
        ChatSession session = sessions.get(sessionId);
        if (session == null) return Optional.empty();
        return Optional.of(session);
    }

    /**
     * Delete a session explicitly (e.g. user closes the chat).
     */
    public void deleteSession(String sessionId) {
        sessions.remove(sessionId);
    }

    /**
     * Returns the total number of active in-memory sessions (useful for monitoring).
     */
    public int activeSessionCount() {
        return sessions.size();
    }

    // ─────────────────────────── Background Eviction ───────────────────────

    /**
     * Evict sessions that haven't been accessed in SESSION_TTL.
     * Runs every 30 minutes.
     */
    @Scheduled(fixedRate = 30 * 60 * 1000)
    public void evictStaleSessions() {
        Instant cutoff = Instant.now().minus(SESSION_TTL);
        sessions.entrySet().removeIf(e -> e.getValue().getLastAccessedAt().isBefore(cutoff));
    }
}
