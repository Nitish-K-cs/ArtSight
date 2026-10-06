package com.artSight.backend.dto;

import com.artSight.backend.entity.Artwork;
import lombok.Data;

import java.util.List;

@Data
public class ArtworkChatReply {
    private String reply;
    private List<RelatedArtwork> relatedArtworks;

    @Data
    public static class RelatedArtwork {
        private Long id;
        private String title;
        private String artist;
        private String imageUrl;
        private String reason;

        public RelatedArtwork(Artwork a, String reason) {
            this.id = a.getId();
            this.title = a.getTitle();
            this.artist = a.getArtist();
            this.imageUrl = a.getImageUrl();
            this.reason = reason;
        }
    }
}
