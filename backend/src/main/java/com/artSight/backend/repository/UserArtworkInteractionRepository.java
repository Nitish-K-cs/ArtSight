package com.artSight.backend.repository;

import com.artSight.backend.entity.UserArtworkInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserArtworkInteractionRepository extends JpaRepository<UserArtworkInteraction, Long> {

    /** All interactions for a user, newest first. */
    List<UserArtworkInteraction> findByUserEmailOrderByInteractedAtDesc(String userEmail);

    /** Check if a specific interaction already exists (for dedup if needed). */
    boolean existsByUserEmailAndArtwork_IdAndType(
            String userEmail, Long artworkId, UserArtworkInteraction.InteractionType type);

    /**
     * Get the distinct artwork IDs a user has interacted with.
     * Used to exclude already-seen artworks from recommendations.
     */
    @Query("SELECT DISTINCT i.artwork.id FROM UserArtworkInteraction i WHERE i.userEmail = :email")
    List<Long> findDistinctArtworkIdsByUserEmail(@Param("email") String email);

    /**
     * Count interactions per artwork for a user — higher count = stronger interest.
     * Returns [artworkId, count] pairs ordered by count desc.
     */
    @Query("""
            SELECT i.artwork.id, COUNT(i) as cnt
            FROM UserArtworkInteraction i
            WHERE i.userEmail = :email
            GROUP BY i.artwork.id
            ORDER BY cnt DESC
            """)
    List<Object[]> findArtworkInteractionCountsByUser(@Param("email") String email);
}
