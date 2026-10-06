package com.artSight.backend.repository;

import com.artSight.backend.entity.Artwork;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArtworkRepository extends JpaRepository<Artwork, Long> {
    Optional<Artwork> findBySourceId(String sourceId);

    /** Find up to 10 artworks in the same department, excluding the given artwork. */
    List<Artwork> findTop10ByDepartmentAndIdNot(String department, Long id);

    /** Find up to 10 artworks from the same culture, excluding the given artwork. */
    List<Artwork> findTop10ByCultureAndIdNot(String culture, Long id);
}