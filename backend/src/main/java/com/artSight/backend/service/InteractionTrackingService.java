package com.artSight.backend.service;

import com.artSight.backend.entity.Artwork;
import com.artSight.backend.entity.UserArtworkInteraction;
import com.artSight.backend.repository.UserArtworkInteractionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Records user interaction events that feed the recommendation engine.
 * Called from ArtworkController (SCAN) and ChatController (CHAT).
 */
@Service
public class InteractionTrackingService {

    @Autowired
    private UserArtworkInteractionRepository interactionRepository;

    /**
     * Record that a user successfully scanned/recognized an artwork.
     */
    public void recordScan(String userEmail, Artwork artwork) {
        record(userEmail, artwork, UserArtworkInteraction.InteractionType.SCAN);
    }

    /**
     * Record that a user opened a chat session about an artwork.
     */
    public void recordChat(String userEmail, Artwork artwork) {
        record(userEmail, artwork, UserArtworkInteraction.InteractionType.CHAT);
    }

    private void record(String userEmail, Artwork artwork, UserArtworkInteraction.InteractionType type) {
        UserArtworkInteraction interaction = new UserArtworkInteraction();
        interaction.setUserEmail(userEmail);
        interaction.setArtwork(artwork);
        interaction.setType(type);
        interactionRepository.save(interaction);
    }
}
