package com.artSight.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Tracks every time a user interacts with an artwork.
 * Each row = one interaction event (SCAN or CHAT).
 * Multiple interactions with the same artwork are allowed —
 * frequency is a signal of interest strength.
 */
@Entity
@Table(name = "user_artwork_interactions", indexes = {
        @Index(name = "idx_interaction_user", columnList = "user_id"),
        @Index(name = "idx_interaction_artwork", columnList = "artwork_id")
})
@Data
public class UserArtworkInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The user who interacted. */
    @Column(name = "user_email", nullable = false)
    private String userEmail;

    /** The artwork they interacted with. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artwork_id", nullable = false)
    private Artwork artwork;

    /** What kind of interaction triggered this record. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InteractionType type;

    @Column(nullable = false, updatable = false)
    private LocalDateTime interactedAt = LocalDateTime.now();

    public enum InteractionType {
        SCAN,   // user recognized the artwork via camera
        CHAT    // user opened a chat session about the artwork
    }
}
