package com.artSight.backend.repository;

import com.artSight.backend.entity.Artwork;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ArtworkRepository extends JpaRepository<Artwork, Long> {
    Optional<Artwork> findBySourceId(String sourceId);
}