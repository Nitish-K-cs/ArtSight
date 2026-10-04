package com.artSight.backend.service;

import com.artSight.backend.dto.met.MetObjectResponse;
import com.artSight.backend.dto.met.MetSearchResponse;
import com.artSight.backend.entity.Artwork;
import com.artSight.backend.repository.ArtworkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class MetIngestionService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String BASE_URL = "https://collectionapi.metmuseum.org/public/collection";

    @Autowired
    private ArtworkRepository artworkRepository;

    public int ingestByQuery(String query, int limit) {
        String searchUrl = BASE_URL + "/v1.1/search?hasImages=true&isPublicDomain=true&q=" + query + "&offset=0&limit=" + Math.min(limit, 500);
        MetSearchResponse searchResponse = restTemplate.getForObject(searchUrl, MetSearchResponse.class);

        if (searchResponse == null || searchResponse.getObjectIDs() == null) {
            return 0;
        }

        List<Long> ids = searchResponse.getObjectIDs();
        int count = 0;

        for (Long objectId : ids) {
            if (count >= limit) break;

            if (artworkRepository.findBySourceId(String.valueOf(objectId)).isPresent()) {
                continue; // already ingested
            }

            try {
                MetObjectResponse obj = restTemplate.getForObject(
                    BASE_URL + "/v1/objects/" + objectId, MetObjectResponse.class);

                if (obj == null) {
                    System.err.println("Skipped object " + objectId + ": null response");
                    continue;
                }

                if (obj.getPrimaryImage() == null || obj.getPrimaryImage().isEmpty()) {
                    System.err.println("Skipped object " + objectId + " (" + obj.getTitle() + "): no usable image");
                    continue;
                }

                Artwork artwork = new Artwork();
                artwork.setSourceId(String.valueOf(obj.getObjectID()));
                artwork.setTitle(obj.getTitle());
                artwork.setArtist(obj.getArtistDisplayName());
                artwork.setDateCreated(obj.getObjectDate());
                artwork.setMedium(obj.getMedium());
                artwork.setCulture(obj.getCulture());
                artwork.setDepartment(obj.getDepartment());
                artwork.setDescription(obj.getCreditLine());
                artwork.setImageUrl(obj.getPrimaryImage());

                artworkRepository.save(artwork);
                count++;

            } catch (Exception e) {
                // Log and skip bad entries rather than failing the whole batch
                System.err.println("Skipped object " + objectId + ": " + e.getMessage());
            }
        }

        return count;
    }
}